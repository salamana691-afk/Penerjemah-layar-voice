import { useMemo, useState } from 'react';
import { Play, Plus, Trash2 } from 'lucide-react';

interface CharacterConfig {
  id: number;
  characterName: string;
  voiceProvider: string;
  voiceId: string;
  pitch: number;
  speechRate: number;
  volume: number;
  enabled: boolean;
  style: string;
}

const INITIAL_CHARACTERS: CharacterConfig[] = [
  {
    id: 1,
    characterName: 'DEFAULT_NARRATOR',
    voiceProvider: 'ANDROID_TTS',
    voiceId: 'id-id-x-idd-local',
    pitch: 1.0,
    speechRate: 1.0,
    volume: 1.0,
    enabled: true,
    style: '',
  },
  {
    id: 2,
    characterName: 'NORA',
    voiceProvider: 'EDGE_COMPATIBLE',
    voiceId: 'id-ID-GadisNeural',
    pitch: 1.1,
    speechRate: 1.05,
    volume: 1.0,
    enabled: true,
    style: 'cheerful',
  },
  {
    id: 3,
    characterName: 'SALMAN',
    voiceProvider: 'EDGE_COMPATIBLE',
    voiceId: 'id-ID-ArdiNeural',
    pitch: 0.95,
    speechRate: 0.95,
    volume: 1.0,
    enabled: true,
    style: 'calm',
  },
];

function escapeXml(input: string): string {
  return input
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&apos;');
}

function formatRelPct(multiplier: number): string {
  const pct = Math.round((Math.min(Math.max(multiplier, 0.5), 2.0) - 1.0) * 100);
  if (pct > 0) return `+${pct}%`;
  if (pct < 0) return `${pct}%`;
  return '0%';
}

export function VoiceSsmlStudio() {
  const [characters, setCharacters] = useState<CharacterConfig[]>(INITIAL_CHARACTERS);
  const [selectedId, setSelectedId] = useState<number>(2);
  const [sampleDialogue, setSampleDialogue] = useState(
    'Selamat datang di perpustakaan senja. Apakah kamu sudah siap membaca catatan kuno ini?'
  );
  const [queuePolicy, setQueuePolicy] = useState<'REPLACE_CURRENT' | 'FINISH_CURRENT' | 'QUEUE_ALL'>(
    'REPLACE_CURRENT'
  );
  const [newCharName, setNewCharName] = useState('');
  const [speakStatus, setSpeakStatus] = useState<string>('');

  const selected = useMemo(
    () => characters.find((c) => c.id === selectedId) ?? characters[0],
    [characters, selectedId]
  );

  const generatedSsml = useMemo(() => {
    const safeVoice = selected.voiceId.replace(/[^A-Za-z0-9._:-]/g, '') || 'id-ID-GadisNeural';
    const pitchStr = formatRelPct(selected.pitch);
    const rateStr = formatRelPct(selected.speechRate);
    const escapedText = escapeXml(sampleDialogue.trim());
    const safeStyle = selected.style.replace(/[^A-Za-z0-9_-]/g, '');

    const prosody = `      <prosody pitch="${pitchStr}" rate="${rateStr}">${escapedText}</prosody>`;
    const inner = safeStyle
      ? `    <mstts:express-as style="${safeStyle}">\n${prosody}\n    </mstts:express-as>`
      : prosody;

    const ns = safeStyle
      ? ' xmlns="http://www.w3.org/2001/10/synthesis" xmlns:mstts="https://www.w3.org/2001/mstts"'
      : ' xmlns="http://www.w3.org/2001/10/synthesis"';

    return `<speak version="1.0"${ns} xml:lang="id-ID">\n  <voice name="${safeVoice}">\n${inner}\n  </voice>\n</speak>`;
  }, [selected, sampleDialogue]);

  const updateSelected = (patch: Partial<CharacterConfig>) => {
    setCharacters((prev) =>
      prev.map((c) => (c.id === selected.id ? { ...c, ...patch } : c))
    );
  };

  const addCharacter = () => {
    const clean = newCharName.trim().toUpperCase();
    if (!clean) return;
    if (characters.some((c) => c.characterName === clean)) {
      setSpeakStatus(`Karakter ${clean} sudah ada.`);
      return;
    }
    const nextId = Math.max(...characters.map((c) => c.id)) + 1;
    const created: CharacterConfig = {
      id: nextId,
      characterName: clean,
      voiceProvider: 'EDGE_COMPATIBLE',
      voiceId: 'id-ID-GadisNeural',
      pitch: 1.0,
      speechRate: 1.0,
      volume: 1.0,
      enabled: true,
      style: '',
    };
    setCharacters((prev) => [...prev, created]);
    setSelectedId(nextId);
    setNewCharName('');
    setSpeakStatus(`Profil ${clean} ditambahkan.`);
  };

  const deleteCharacter = (id: number) => {
    const target = characters.find((c) => c.id === id);
    if (!target || target.characterName === 'DEFAULT_NARRATOR') return;
    const next = characters.filter((c) => c.id !== id);
    setCharacters(next);
    if (selectedId === id && next[0]) {
      setSelectedId(next[0].id);
    }
  };

  const handleBrowserTestVoice = () => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      setSpeakStatus('Browser Web Speech API tidak tersedia di lingkungan ini.');
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(sampleDialogue);
    utterance.lang = 'id-ID';
    utterance.pitch = selected.pitch;
    utterance.rate = selected.speechRate;
    utterance.volume = selected.volume;
    utterance.onstart = () => setSpeakStatus(`Memutar pratinjau untuk ${selected.characterName}...`);
    utterance.onend = () => setSpeakStatus(`Pratinjau ${selected.characterName} selesai.`);
    utterance.onerror = () => setSpeakStatus('Pemutaran audio browser tidak didukung pada perangkat ini.');
    window.speechSynthesis.speak(utterance);
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold text-[#F8FAFC]">
            Multi-Character Voice Database &amp; SSML Studio
          </h2>
          <p className="text-sm text-[#94A3B8]">
            Simulasikan konfigurasi Room `CharacterProfileEntity`, validasi `SSMLGenerator` anti-XML injection, dan kebijakan antrean ExoPlayer.
          </p>
        </div>

        <div className="flex items-center gap-2">
          <input
            type="text"
            value={newCharName}
            onChange={(e) => setNewCharName(e.target.value)}
            placeholder="Nama karakter baru (mis. LUCY)"
            className="px-3 py-2 rounded-lg bg-[#111827] border border-[#24304A] text-xs text-[#F8FAFC] placeholder-[#64748B] focus:outline-none focus:border-[#00E5FF]"
          />
          <button
            type="button"
            onClick={addCharacter}
            className="px-3 py-2 rounded-lg bg-[#00E5FF] text-[#0A0F1E] text-xs font-semibold hover:bg-[#38EDFF] inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
          >
            <Plus className="w-3.5 h-3.5" />
            <span>Tambah</span>
          </button>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <div className="lg:col-span-4 bg-[#111827] border border-[#24304A] rounded-xl p-4 space-y-2">
          <h3 className="text-sm font-semibold text-[#F8FAFC] px-1 pb-1">
            Daftar Profil Karakter
          </h3>
          {characters.map((char) => {
            const active = char.id === selected.id;
            return (
              <div
                key={char.id}
                onClick={() => setSelectedId(char.id)}
                className={`p-3 rounded-lg border flex items-center justify-between gap-2 cursor-pointer transition-colors ${
                  active
                    ? 'bg-[#161F33] border-[#00E5FF]'
                    : 'bg-[#0A0F1E]/60 border-[#24304A] hover:border-[#334155]'
                }`}
              >
                <div className="min-w-0">
                  <div className="text-sm font-semibold text-[#F8FAFC] truncate">
                    {char.characterName === 'DEFAULT_NARRATOR' ? 'Default Narrator' : char.characterName}
                  </div>
                  <div className="text-[11px] font-mono text-[#94A3B8] truncate">
                    {char.voiceId} · {formatRelPct(char.pitch)} / {formatRelPct(char.speechRate)}
                  </div>
                </div>

                {char.characterName !== 'DEFAULT_NARRATOR' && (
                  <button
                    type="button"
                    onClick={(e) => {
                      e.stopPropagation();
                      deleteCharacter(char.id);
                    }}
                    title="Hapus karakter"
                    className="p-1.5 rounded text-[#94A3B8] hover:text-[#EF4444] cursor-pointer"
                  >
                    <Trash2 className="w-4 h-4" />
                  </button>
                )}
              </div>
            );
          })}
        </div>

        <div className="lg:col-span-4 bg-[#111827] border border-[#24304A] rounded-xl p-5 space-y-4">
          <h3 className="text-sm font-semibold text-[#F8FAFC]">
            Konfigurasi Suara: {selected.characterName}
          </h3>

          <div className="space-y-3 text-xs">
            <label className="block space-y-1">
              <span className="text-[#94A3B8]">Voice Provider</span>
              <select
                value={selected.voiceProvider}
                onChange={(e) => updateSelected({ voiceProvider: e.target.value })}
                className="w-full px-3 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC]"
              >
                <option value="EDGE_COMPATIBLE">Edge-Compatible WebSocket TTS</option>
                <option value="ANDROID_TTS">Android Native TextToSpeech (Offline)</option>
              </select>
            </label>

            <label className="block space-y-1">
              <span className="text-[#94A3B8]">Voice ID</span>
              <input
                type="text"
                value={selected.voiceId}
                onChange={(e) => updateSelected({ voiceId: e.target.value })}
                className="w-full px-3 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC] font-mono"
              />
            </label>

            <label className="block space-y-1">
              <div className="flex justify-between text-[#94A3B8]">
                <span>Pitch</span>
                <span className="font-mono tabular-nums text-[#00E5FF]">
                  {selected.pitch.toFixed(2)}x ({formatRelPct(selected.pitch)})
                </span>
              </div>
              <input
                type="range"
                min={0.5}
                max={2.0}
                step={0.05}
                value={selected.pitch}
                onChange={(e) => updateSelected({ pitch: parseFloat(e.target.value) })}
                className="w-full accent-[#00E5FF]"
              />
            </label>

            <label className="block space-y-1">
              <div className="flex justify-between text-[#94A3B8]">
                <span>Speech Rate</span>
                <span className="font-mono tabular-nums text-[#00E5FF]">
                  {selected.speechRate.toFixed(2)}x ({formatRelPct(selected.speechRate)})
                </span>
              </div>
              <input
                type="range"
                min={0.5}
                max={2.0}
                step={0.05}
                value={selected.speechRate}
                onChange={(e) => updateSelected({ speechRate: parseFloat(e.target.value) })}
                className="w-full accent-[#00E5FF]"
              />
            </label>

            <label className="block space-y-1">
              <div className="flex justify-between text-[#94A3B8]">
                <span>Volume</span>
                <span className="font-mono tabular-nums text-[#F8FAFC]">
                  {(selected.volume * 100).toFixed(0)}%
                </span>
              </div>
              <input
                type="range"
                min={0}
                max={1}
                step={0.05}
                value={selected.volume}
                onChange={(e) => updateSelected({ volume: parseFloat(e.target.value) })}
                className="w-full accent-[#A855F7]"
              />
            </label>

            <label className="block space-y-1">
              <span className="text-[#94A3B8]">Audio Queue Policy</span>
              <select
                value={queuePolicy}
                onChange={(e) => setQueuePolicy(e.target.value as typeof queuePolicy)}
                className="w-full px-3 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC]"
              >
                <option value="REPLACE_CURRENT">Replace Current (Default)</option>
                <option value="FINISH_CURRENT">Finish Current</option>
                <option value="QUEUE_ALL">Queue All</option>
              </select>
            </label>
          </div>

          <button
            type="button"
            onClick={handleBrowserTestVoice}
            className="w-full py-2.5 px-4 rounded-lg bg-[#00E5FF] text-[#0A0F1E] text-xs font-semibold hover:bg-[#38EDFF] inline-flex items-center justify-center gap-2 cursor-pointer"
          >
            <Play className="w-3.5 h-3.5" />
            <span>Test Voice (Web Speech Preview)</span>
          </button>

          {speakStatus && <p className="text-xs text-[#94A3B8]">{speakStatus}</p>}
        </div>

        <div className="lg:col-span-4 bg-[#111827] border border-[#24304A] rounded-xl p-5 space-y-3">
          <h3 className="text-sm font-semibold text-[#F8FAFC]">Pratinjau Output SSMLGenerator</h3>
          <textarea
            rows={3}
            value={sampleDialogue}
            onChange={(e) => setSampleDialogue(e.target.value)}
            className="w-full p-2.5 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-xs text-[#F8FAFC] focus:outline-none focus:border-[#00E5FF]"
          />
          <pre className="p-3 rounded-lg bg-[#070B14] border border-[#24304A] text-[11px] font-mono text-[#00E5FF] overflow-x-auto leading-relaxed">
            {generatedSsml}
          </pre>
          <p className="text-[11px] text-[#94A3B8]">
            Karakter XML seperti &lt;, &gt;, &amp;, &quot;, dan &apos; di-escape otomatis; voice ID dan rentang pitch/rate divalidasi sebelum dikirim ke provider.
          </p>
        </div>
      </div>
    </div>
  );
}
