import { useMemo, useState } from 'react';
import { Eye, EyeOff, RotateCcw, SlidersHorizontal, Volume2, VolumeX } from 'lucide-react';

interface RoiCoords {
  left: number;
  top: number;
  width: number;
  height: number;
  alpha: number;
  color: string;
}

interface SampleScene {
  id: string;
  label: string;
  rawNameOcr: string;
  rawDialogOcr: string;
  translatedId: string;
}

const SAMPLE_SCENES: SampleScene[] = [
  {
    id: 'nora',
    label: 'Scene 1 · Nora',
    rawNameOcr: '[ Nora ]',
    rawDialogOcr: "I didn't expect to\nfind you in the archive\nthis late at night.",
    translatedId: 'Aku tidak menyangka akan menemukanmu di ruang arsip selarut ini.',
  },
  {
    id: 'salman_typo',
    label: 'Scene 2 · OCR Typo (SALM4N)',
    rawNameOcr: 'SALM4N :',
    rawDialogOcr: 'The lantern seal is\nfading. We must leave\nbefore dawn breaks.',
    translatedId: 'Segel lentera mulai memudar. Kita harus pergi sebelum fajar menyingsing.',
  },
  {
    id: 'missing_name',
    label: 'Scene 3 · Nama Hilang Sementara',
    rawNameOcr: '',
    rawDialogOcr: 'Wait... did you hear\nfootsteps behind the\nwooden bookshelf?',
    translatedId: 'Tunggu... apakah kamu mendengar langkah kaki di balik rak buku kayu itu?',
  },
  {
    id: 'unknown',
    label: 'Scene 4 · Karakter Baru',
    rawNameOcr: 'LIBRARIAN',
    rawDialogOcr: 'Please keep your voices\nquiet inside the grand\nhall, travelers.',
    translatedId: 'Mohon pelankan suara kalian di dalam aula utama, para pengembara.',
  },
];

const DEFAULT_NAME_ROI: RoiCoords = {
  left: 0.10,
  top: 0.67,
  width: 0.21,
  height: 0.065,
  alpha: 0.30,
  color: '#00E5FF',
};

const DEFAULT_DIALOG_ROI: RoiCoords = {
  left: 0.10,
  top: 0.75,
  width: 0.79,
  height: 0.18,
  alpha: 0.26,
  color: '#A855F7',
};

function normalizeName(raw: string): string {
  return raw
    .replace(/[\[\](){}<>|_~`*=•·:;"]+/g, ' ')
    .replace(/\s+/g, ' ')
    .trim()
    .toUpperCase();
}

function normalizeDialog(raw: string): string {
  return raw
    .replace(/\r\n/g, '\n')
    .split('\n')
    .map((l) => l.trim())
    .filter(Boolean)
    .join(' ');
}

function levenshtein(a: string, b: string): number {
  const m = a.length;
  const n = b.length;
  const dp = Array.from({ length: m + 1 }, () => new Array<number>(n + 1).fill(0));
  for (let i = 0; i <= m; i++) dp[i][0] = i;
  for (let j = 0; j <= n; j++) dp[0][j] = j;
  for (let i = 1; i <= m; i++) {
    for (let j = 1; j <= n; j++) {
      const cost = a[i - 1] === b[j - 1] ? 0 : 1;
      dp[i][j] = Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1, dp[i - 1][j - 1] + cost);
    }
  }
  return dp[m][n];
}

export function RoiSimulator() {
  const [setupMode, setSetupMode] = useState(true);
  const [activeBox, setActiveBox] = useState<'name' | 'dialog'>('name');
  const [nameRoi, setNameRoi] = useState<RoiCoords>(DEFAULT_NAME_ROI);
  const [dialogRoi, setDialogRoi] = useState<RoiCoords>(DEFAULT_DIALOG_ROI);
  const [sceneId, setSceneId] = useState<string>('nora');
  const [overlayVisible, setOverlayVisible] = useState(true);
  const [voiceEnabled, setVoiceEnabled] = useState(true);
  const [overlayOpacity, setOverlayOpacity] = useState(0.84);
  const [overlayFontSize, setOverlayFontSize] = useState(15);
  const [imgError, setImgError] = useState(false);

  const currentScene = useMemo(
    () => SAMPLE_SCENES.find((s) => s.id === sceneId) ?? SAMPLE_SCENES[0],
    [sceneId]
  );

  const matchEvaluation = useMemo(() => {
    const cleanedName = normalizeName(currentScene.rawNameOcr);
    const cleanedDialog = normalizeDialog(currentScene.rawDialogOcr);
    const known = [
      { name: 'NORA', voice: 'id-ID-GadisNeural', pitch: '+10%', rate: '+5%' },
      { name: 'SALMAN', voice: 'id-ID-ArdiNeural', pitch: '-5%', rate: '-5%' },
    ];

    if (!cleanedName) {
      return {
        cleanedName: '(kosong — pertahankan pembicara sebelumnya)',
        cleanedDialog,
        matchedProfile: 'NORA (Preserved)',
        voice: 'id-ID-GadisNeural · Pitch +10% · Rate +5%',
        matchType: 'PREVIOUS_SPEAKER_PRESERVED',
      };
    }

    const exact = known.find((k) => k.name === cleanedName);
    if (exact) {
      return {
        cleanedName,
        cleanedDialog,
        matchedProfile: exact.name,
        voice: `${exact.voice} · Pitch ${exact.pitch} · Rate ${exact.rate}`,
        matchType: 'EXACT',
      };
    }

    const scored = known
      .map((k) => {
        const dist = levenshtein(cleanedName, k.name);
        const sim = 1 - dist / Math.max(cleanedName.length, k.name.length);
        return { ...k, sim };
      })
      .sort((a, b) => b.sim - a.sim);

    if (scored[0] && scored[0].sim >= 0.78) {
      return {
        cleanedName,
        cleanedDialog,
        matchedProfile: `${scored[0].name} (Sim ${(scored[0].sim * 100).toFixed(0)}%)`,
        voice: `${scored[0].voice} · Pitch ${scored[0].pitch} · Rate ${scored[0].rate}`,
        matchType: 'FUZZY',
      };
    }

    return {
      cleanedName,
      cleanedDialog,
      matchedProfile: 'DEFAULT_NARRATOR',
      voice: 'System Default · Pitch 0% · Rate 0%',
      matchType: 'DEFAULT_NARRATOR',
    };
  }, [currentScene]);

  const currentRoi = activeBox === 'name' ? nameRoi : dialogRoi;
  const updateCurrentRoi = (patch: Partial<RoiCoords>) => {
    const clamp = (c: RoiCoords): RoiCoords => {
      const w = Math.min(Math.max(c.width, 0.08), 0.96);
      const h = Math.min(Math.max(c.height, 0.05), 0.65);
      const l = Math.min(Math.max(c.left, 0), 1 - w);
      const t = Math.min(Math.max(c.top, 0), 1 - h);
      return { ...c, width: w, height: h, left: l, top: t };
    };
    if (activeBox === 'name') {
      setNameRoi((prev) => clamp({ ...prev, ...patch }));
    } else {
      setDialogRoi((prev) => clamp({ ...prev, ...patch }));
    }
  };

  const resetDefaults = () => {
    setNameRoi(DEFAULT_NAME_ROI);
    setDialogRoi(DEFAULT_DIALOG_ROI);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold text-[#F8FAFC]">
            Simulator Dual-Box OCR &amp; Floating Overlay
          </h2>
          <p className="text-sm text-[#94A3B8]">
            Uji posisi ROI 1 (Nama Karakter) dan ROI 2 (Dialog) serta pratinjau hasil normalisasi &amp; overlay terjemahan.
          </p>
        </div>

        <div className="flex items-center gap-1 p-1 bg-[#111827] border border-[#24304A] rounded-lg self-start">
          <button
            type="button"
            onClick={() => setSetupMode(true)}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
              setupMode ? 'bg-[#00E5FF] text-[#0A0F1E]' : 'text-[#94A3B8] hover:text-[#F8FAFC]'
            }`}
          >
            Setup Mode
          </button>
          <button
            type="button"
            onClick={() => setSetupMode(false)}
            className={`px-3 py-1.5 text-xs font-medium rounded-md transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
              !setupMode ? 'bg-[#00E5FF] text-[#0A0F1E]' : 'text-[#94A3B8] hover:text-[#F8FAFC]'
            }`}
          >
            Scan Mode
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <div className="lg:col-span-8 space-y-4">
          <div className="relative w-full aspect-video rounded-xl overflow-hidden border border-[#24304A] bg-[#070B14] select-none">
            {!imgError ? (
              <img
                src="/src/assets/images/vn_scene_preview_1794886839177.jpg"
                alt="Visual Novel game preview scene"
                referrerPolicy="no-referrer"
                onError={() => setImgError(true)}
                className="w-full h-full object-cover"
              />
            ) : (
              <div className="w-full h-full bg-gradient-to-br from-[#0F172A] via-[#111827] to-[#1E1B4B] flex items-center justify-center text-sm text-[#94A3B8]">
                Visual Novel Canvas Preview (16:9)
              </div>
            )}

            {/* Simulated VN in-game nameplate & dialog text inside the bottom box */}
            <div
              className="absolute pointer-events-none px-2 py-0.5 text-xs sm:text-sm font-bold tracking-wide text-[#00E5FF]"
              style={{ left: '11.5%', top: '68%' }}
            >
              {currentScene.rawNameOcr || ' '}
            </div>
            <div
              className="absolute pointer-events-none text-xs sm:text-sm text-[#F8FAFC] leading-snug max-w-[75%]"
              style={{ left: '11.5%', top: '76.5%' }}
            >
              {currentScene.rawDialogOcr.split('\n').map((line, i) => (
                <div key={i}>{line}</div>
              ))}
            </div>

            {/* Floating Translation Overlay */}
            {overlayVisible && (
              <div
                className="absolute left-[7%] top-[7%] max-w-[78%] rounded-xl border border-[#00E5FF]/50 px-4 py-2.5 shadow-lg transition-opacity"
                style={{
                  backgroundColor: `rgba(10, 15, 30, ${overlayOpacity})`,
                }}
              >
                <div className="flex items-center justify-between gap-4 mb-1">
                  <span className="text-xs font-bold text-[#00E5FF]">
                    {matchEvaluation.matchedProfile}
                  </span>
                  <span className="text-[11px] font-mono text-[#94A3B8]">
                    {voiceEnabled ? 'Voice On' : 'Muted'}
                  </span>
                </div>
                <p
                  className="text-[#F8FAFC] font-medium leading-snug"
                  style={{ fontSize: `${overlayFontSize}px` }}
                >
                  {currentScene.translatedId}
                </p>
              </div>
            )}

            {/* Floating Control Bar Simulation */}
            <div className="absolute right-3 top-3 flex items-center gap-1 px-2 py-1.5 rounded-lg bg-[#0B1120]/90 border border-[#00E5FF]/40">
              <button
                type="button"
                onClick={() => setOverlayVisible((v) => !v)}
                title="Show / Hide Translation"
                className="p-1.5 rounded hover:bg-[#1E293B] text-[#00E5FF] cursor-pointer"
              >
                {overlayVisible ? <Eye className="w-3.5 h-3.5" /> : <EyeOff className="w-3.5 h-3.5 text-[#64748B]" />}
              </button>
              <button
                type="button"
                onClick={() => setVoiceEnabled((v) => !v)}
                title="Enable / Disable Voice"
                className="p-1.5 rounded hover:bg-[#1E293B] text-[#A855F7] cursor-pointer"
              >
                {voiceEnabled ? <Volume2 className="w-3.5 h-3.5" /> : <VolumeX className="w-3.5 h-3.5 text-[#64748B]" />}
              </button>
            </div>

            {/* Dual-Box ROI Overlays visible in Setup Mode */}
            {setupMode && (
              <>
                <div
                  onClick={() => setActiveBox('name')}
                  className="absolute rounded-md cursor-pointer transition-all"
                  style={{
                    left: `${nameRoi.left * 100}%`,
                    top: `${nameRoi.top * 100}%`,
                    width: `${nameRoi.width * 100}%`,
                    height: `${nameRoi.height * 100}%`,
                    border: `2px solid ${nameRoi.color}`,
                    backgroundColor: `rgba(0, 229, 255, ${nameRoi.alpha * 0.45})`,
                  }}
                >
                  <span className="inline-block px-1.5 py-0.5 text-[10px] font-semibold bg-[#0A0F1E]/85 text-[#F8FAFC] rounded-br">
                    ROI 1: Nama Karakter
                  </span>
                </div>

                <div
                  onClick={() => setActiveBox('dialog')}
                  className="absolute rounded-md cursor-pointer transition-all"
                  style={{
                    left: `${dialogRoi.left * 100}%`,
                    top: `${dialogRoi.top * 100}%`,
                    width: `${dialogRoi.width * 100}%`,
                    height: `${dialogRoi.height * 100}%`,
                    border: `2px solid ${dialogRoi.color}`,
                    backgroundColor: `rgba(168, 85, 247, ${dialogRoi.alpha * 0.45})`,
                  }}
                >
                  <span className="inline-block px-1.5 py-0.5 text-[10px] font-semibold bg-[#0A0F1E]/85 text-[#F8FAFC] rounded-br">
                    ROI 2: Dialog Detector
                  </span>
                </div>
              </>
            )}
          </div>

          <div className="flex flex-wrap items-center gap-2">
            {SAMPLE_SCENES.map((sc) => (
              <button
                key={sc.id}
                type="button"
                onClick={() => setSceneId(sc.id)}
                className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
                  sceneId === sc.id
                    ? 'bg-[#161F33] border-[#00E5FF] text-[#00E5FF]'
                    : 'bg-[#111827] border-[#24304A] text-[#94A3B8] hover:text-[#F8FAFC]'
                }`}
              >
                {sc.label}
              </button>
            ))}
          </div>
        </div>

        <div className="lg:col-span-4 space-y-4">
          <div className="bg-[#111827] border border-[#24304A] rounded-xl p-5 space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-sm font-semibold text-[#F8FAFC]">
                <SlidersHorizontal className="w-4 h-4 text-[#00E5FF]" />
                <span>Pengaturan Kotak ROI</span>
              </div>
              <button
                type="button"
                onClick={resetDefaults}
                className="text-xs text-[#94A3B8] hover:text-[#F8FAFC] inline-flex items-center gap-1 cursor-pointer"
              >
                <RotateCcw className="w-3.5 h-3.5" />
                <span>Reset</span>
              </button>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <button
                type="button"
                onClick={() => setActiveBox('name')}
                className={`py-2 px-3 rounded-lg text-xs font-medium border cursor-pointer ${
                  activeBox === 'name'
                    ? 'bg-[#00E5FF]/15 border-[#00E5FF] text-[#00E5FF]'
                    : 'bg-[#161F33] border-[#24304A] text-[#94A3B8]'
                }`}
              >
                ROI 1 (Nama)
              </button>
              <button
                type="button"
                onClick={() => setActiveBox('dialog')}
                className={`py-2 px-3 rounded-lg text-xs font-medium border cursor-pointer ${
                  activeBox === 'dialog'
                    ? 'bg-[#A855F7]/15 border-[#A855F7] text-[#A855F7]'
                    : 'bg-[#161F33] border-[#24304A] text-[#94A3B8]'
                }`}
              >
                ROI 2 (Dialog)
              </button>
            </div>

            <div className="space-y-3 text-xs">
              <label className="block space-y-1">
                <div className="flex justify-between text-[#94A3B8]">
                  <span>Posisi X</span>
                  <span className="font-mono tabular-nums text-[#F8FAFC]">
                    {(currentRoi.left * 100).toFixed(0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={0.85}
                  step={0.01}
                  value={currentRoi.left}
                  onChange={(e) => updateCurrentRoi({ left: parseFloat(e.target.value) })}
                  className="w-full accent-[#00E5FF]"
                />
              </label>

              <label className="block space-y-1">
                <div className="flex justify-between text-[#94A3B8]">
                  <span>Posisi Y</span>
                  <span className="font-mono tabular-nums text-[#F8FAFC]">
                    {(currentRoi.top * 100).toFixed(0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0}
                  max={0.90}
                  step={0.01}
                  value={currentRoi.top}
                  onChange={(e) => updateCurrentRoi({ top: parseFloat(e.target.value) })}
                  className="w-full accent-[#00E5FF]"
                />
              </label>

              <label className="block space-y-1">
                <div className="flex justify-between text-[#94A3B8]">
                  <span>Lebar Kotak</span>
                  <span className="font-mono tabular-nums text-[#F8FAFC]">
                    {(currentRoi.width * 100).toFixed(0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0.08}
                  max={0.96}
                  step={0.01}
                  value={currentRoi.width}
                  onChange={(e) => updateCurrentRoi({ width: parseFloat(e.target.value) })}
                  className="w-full accent-[#00E5FF]"
                />
              </label>

              <label className="block space-y-1">
                <div className="flex justify-between text-[#94A3B8]">
                  <span>Tinggi Kotak</span>
                  <span className="font-mono tabular-nums text-[#F8FAFC]">
                    {(currentRoi.height * 100).toFixed(0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0.05}
                  max={0.55}
                  step={0.01}
                  value={currentRoi.height}
                  onChange={(e) => updateCurrentRoi({ height: parseFloat(e.target.value) })}
                  className="w-full accent-[#00E5FF]"
                />
              </label>

              <label className="block space-y-1">
                <div className="flex justify-between text-[#94A3B8]">
                  <span>Overlay Opacity</span>
                  <span className="font-mono tabular-nums text-[#F8FAFC]">
                    {(overlayOpacity * 100).toFixed(0)}%
                  </span>
                </div>
                <input
                  type="range"
                  min={0.2}
                  max={0.96}
                  step={0.02}
                  value={overlayOpacity}
                  onChange={(e) => setOverlayOpacity(parseFloat(e.target.value))}
                  className="w-full accent-[#A855F7]"
                />
              </label>

              <label className="block space-y-1">
                <div className="flex justify-between text-[#94A3B8]">
                  <span>Ukuran Font Overlay</span>
                  <span className="font-mono tabular-nums text-[#F8FAFC]">{overlayFontSize} px</span>
                </div>
                <input
                  type="range"
                  min={12}
                  max={22}
                  step={1}
                  value={overlayFontSize}
                  onChange={(e) => setOverlayFontSize(parseInt(e.target.value, 10))}
                  className="w-full accent-[#A855F7]"
                />
              </label>
            </div>
          </div>

          <div className="bg-[#111827] border border-[#24304A] rounded-xl p-5 space-y-2.5 text-xs">
            <h3 className="text-sm font-semibold text-[#F8FAFC]">Hasil Pemrosesan Pipeline</h3>
            <div className="space-y-1.5">
              <div className="text-[#94A3B8]">
                Nama Ternormalisasi:{' '}
                <span className="font-mono text-[#00E5FF]">{matchEvaluation.cleanedName}</span>
              </div>
              <div className="text-[#94A3B8]">
                Profil Room Cocok:{' '}
                <span className="font-mono text-[#F8FAFC]">{matchEvaluation.matchedProfile}</span>
              </div>
              <div className="text-[#94A3B8]">
                Strategi Pencocokan:{' '}
                <span className="font-mono text-[#22C55E]">{matchEvaluation.matchType}</span>
              </div>
              <div className="text-[#94A3B8]">
                Voice Dipilih: <span className="font-mono text-[#A855F7]">{matchEvaluation.voice}</span>
              </div>
              <div className="pt-1 text-[#94A3B8]">
                Dialog Dibersihkan:{' '}
                <span className="text-[#CBD5E1]">{matchEvaluation.cleanedDialog}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
