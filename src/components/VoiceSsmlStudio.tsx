import { useMemo, useRef, useState } from 'react';
import { Play, Plus, Trash2, Wifi, WifiOff } from 'lucide-react';

interface CharacterConfig {
  id: number;
  characterName: string;
  voiceProvider: 'GEMINI_TTS' | 'EDGE_COMPATIBLE' | 'ANDROID_TTS';
  voiceId: string;
  offlineVoiceId: string;
  personalityId: string;
  emotionId: string;
  customDescription: string;
  pitch: number;
  speechRate: number;
  volume: number;
  enabled: boolean;
  style: string;
}

const PERSONALITIES = [
  {
    id: 'ANIME_FEMALE_GENTLE',
    label: 'Sakura Gentle (Perempuan · Lembut)',
    defaultVoice: 'Kore',
    prompt:
      'Suara perempuan dewasa bergaya anime Jepang yang lembut, hangat, feminin, ramah, dengan artikulasi bahasa Indonesia yang jelas.',
  },
  {
    id: 'ANIME_FEMALE_CHEERFUL',
    label: 'Hana Bright (Perempuan · Ceria & Sedikit Manja)',
    defaultVoice: 'Zephyr',
    prompt:
      'Suara perempuan dewasa bergaya anime Jepang yang lembut, ceria, sedikit manja, energik, dengan intonasi ekspresif dan artikulasi jelas.',
  },
  {
    id: 'ANIME_FEMALE_COOL',
    label: 'Rei Serene (Perempuan · Kalem & Tenang)',
    defaultVoice: 'Aoede',
    prompt:
      'Suara perempuan dewasa bergaya anime Jepang yang kalem, tenang, dewasa, penuh kendali, dan bernada halus.',
  },
  {
    id: 'ANIME_FEMALE_EXPRESSIVE',
    label: 'Aoi Dramatic (Perempuan · Ekspresif)',
    defaultVoice: 'Leda',
    prompt:
      'Suara perempuan dewasa bergaya anime Jepang yang sangat ekspresif, dinamis, penuh penjiwaan visual novel.',
  },
  {
    id: 'ANIME_MALE_CALM',
    label: 'Kenji Calm (Laki-laki · Kalem)',
    defaultVoice: 'Charon',
    prompt:
      'Suara laki-laki dewasa bergaya anime Jepang yang tenang, berwibawa, jernih, rendah, dan stabil.',
  },
  {
    id: 'ANIME_MALE_ENERGETIC',
    label: 'Ren Energetic (Laki-laki · Energik)',
    defaultVoice: 'Puck',
    prompt:
      'Suara laki-laki dewasa bergaya anime Jepang yang energik, bersemangat, tegas, dan bertempo hidup.',
  },
  {
    id: 'ANIME_MALE_MENTOR',
    label: 'Haru Mentor (Laki-laki · Dewasa)',
    defaultVoice: 'Fenrir',
    prompt:
      'Suara laki-laki dewasa yang hangat, dalam, bijaksana, cocok untuk karakter senior atau narator cerita.',
  },
  {
    id: 'CUSTOM_ONLY',
    label: 'Custom Persona (Deskripsi Bebas)',
    defaultVoice: 'Kore',
    prompt: '',
  },
] as const;

const EMOTIONS = [
  { id: 'NEUTRAL', label: 'Natural', directive: 'Intonasi alami dan seimbang.', pitchDelta: 0, rateDelta: 0 },
  { id: 'HAPPY', label: 'Happy', directive: 'Nada gembira, cerah, dan tersenyum saat berbicara.', pitchDelta: 0.06, rateDelta: 0.05 },
  { id: 'SAD', label: 'Sad', directive: 'Nada sedih, lirih, lembut, dan sedikit melambat penuh haru.', pitchDelta: -0.06, rateDelta: -0.08 },
  { id: 'ANGRY', label: 'Angry', directive: 'Nada marah, tegas, intens, dan penekanan kuat.', pitchDelta: 0.04, rateDelta: 0.08 },
  { id: 'EXCITED', label: 'Excited', directive: 'Sangat bersemangat, antusias, ekspresif, dan bertempo cepat.', pitchDelta: 0.10, rateDelta: 0.10 },
  { id: 'WHISPER', label: 'Whisper', directive: 'Berbisik lembut, pelan, intim, dan penuh kehati-hatian.', pitchDelta: -0.04, rateDelta: -0.10 },
  { id: 'CALM', label: 'Calm', directive: 'Tenang, santai, lembut, dan menenangkan.', pitchDelta: -0.02, rateDelta: -0.04 },
  { id: 'DRAMATIC', label: 'Dramatic', directive: 'Dramatis, teatrikal, penuh penjiwaan dan jeda emosional.', pitchDelta: 0, rateDelta: -0.05 },
] as const;

const INITIAL_CHARACTERS: CharacterConfig[] = [
  {
    id: 1,
    characterName: 'DEFAULT_NARRATOR',
    voiceProvider: 'GEMINI_TTS',
    voiceId: 'Charon',
    offlineVoiceId: 'id-id-x-idd-local',
    personalityId: 'ANIME_MALE_CALM',
    emotionId: 'NEUTRAL',
    customDescription: 'Suara narator dewasa yang tenang, jernih, netral, dan mudah dipahami.',
    pitch: 1.0,
    speechRate: 1.0,
    volume: 1.0,
    enabled: true,
    style: '',
  },
  {
    id: 2,
    characterName: 'NORA',
    voiceProvider: 'GEMINI_TTS',
    voiceId: 'Zephyr',
    offlineVoiceId: 'id-id-x-idc-local',
    personalityId: 'ANIME_FEMALE_CHEERFUL',
    emotionId: 'HAPPY',
    customDescription:
      'Suara perempuan dewasa bergaya anime Jepang, lembut, ceria, sedikit manja, dengan intonasi ekspresif dan artikulasi jelas.',
    pitch: 1.08,
    speechRate: 1.04,
    volume: 1.0,
    enabled: true,
    style: 'cheerful',
  },
  {
    id: 3,
    characterName: 'SALMAN',
    voiceProvider: 'GEMINI_TTS',
    voiceId: 'Charon',
    offlineVoiceId: 'id-id-x-idd-local',
    personalityId: 'ANIME_MALE_CALM',
    emotionId: 'CALM',
    customDescription: 'Suara laki-laki dewasa bergaya anime Jepang yang kalem, berwibawa, dan tenang.',
    pitch: 0.95,
    speechRate: 0.96,
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
  const [operatingMode, setOperatingMode] = useState<'AUTOMATIC' | 'MANUAL'>('AUTOMATIC');
  const [simulatedOnline, setSimulatedOnline] = useState<boolean>(true);
  const [geminiModel, setGeminiModel] = useState<'gemini-3.8-flash-tts' | 'gemini-3.8-flash-lite-tts'>(
    'gemini-3.8-flash-tts'
  );
  const [sampleDialogue, setSampleDialogue] = useState(
    'Selamat datang di perpustakaan senja. Apakah kamu sudah siap membaca catatan kuno ini?'
  );
  const [queuePolicy, setQueuePolicy] = useState<'REPLACE_CURRENT' | 'FINISH_CURRENT' | 'QUEUE_ALL'>(
    'REPLACE_CURRENT'
  );
  const [newCharName, setNewCharName] = useState('');
  const [speakStatus, setSpeakStatus] = useState<string>('');
  const [isSynthesizing, setIsSynthesizing] = useState(false);

  const audioCacheRef = useRef<Map<string, string>>(new Map());
  const activeAudioRef = useRef<HTMLAudioElement | null>(null);

  const selected = useMemo(
    () => characters.find((c) => c.id === selectedId) ?? characters[0],
    [characters, selectedId]
  );

  const selectedPersonality = useMemo(
    () => PERSONALITIES.find((p) => p.id === selected.personalityId) ?? PERSONALITIES[0],
    [selected.personalityId]
  );

  const selectedEmotion = useMemo(
    () => EMOTIONS.find((e) => e.id === selected.emotionId) ?? EMOTIONS[0],
    [selected.emotionId]
  );

  const composedStylePrompt = useMemo(() => {
    const parts: string[] = [];
    const custom = selected.customDescription.trim();
    if (custom) {
      parts.push(custom);
    } else if (selectedPersonality.prompt) {
      parts.push(selectedPersonality.prompt);
    }
    parts.push(`Emosi: ${selectedEmotion.label} — ${selectedEmotion.directive}`);
    if (selected.pitch >= 1.15) parts.push('Nada suara sedikit lebih tinggi dan cerah.');
    else if (selected.pitch <= 0.88) parts.push('Nada suara sedikit lebih rendah dan dalam.');
    if (selected.speechRate >= 1.15) parts.push('Tempo bicara agak cepat namun tetap jelas.');
    else if (selected.speechRate <= 0.88) parts.push('Tempo bicara lebih santai dan tenang.');
    parts.push('Bacakan dialog berikut secara natural dalam bahasa Indonesia dengan artikulasi jernih tanpa membaca instruksi ini.');
    return parts.join(' ');
  }, [selected, selectedPersonality, selectedEmotion]);

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
      voiceProvider: 'GEMINI_TTS',
      voiceId: 'Kore',
      offlineVoiceId: 'id-id-x-idd-local',
      personalityId: 'ANIME_FEMALE_GENTLE',
      emotionId: 'NEUTRAL',
      customDescription: '',
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

  const playOfflineFallback = (reasonPrefix: string) => {
    if (typeof window === 'undefined' || !('speechSynthesis' in window)) {
      setSpeakStatus(`${reasonPrefix} Web Speech API tidak tersedia di browser ini.`);
      return;
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(sampleDialogue);
    utterance.lang = 'id-ID';
    utterance.pitch = Math.min(Math.max(selected.pitch + selectedEmotion.pitchDelta, 0.5), 2.0);
    utterance.rate = Math.min(Math.max(selected.speechRate + selectedEmotion.rateDelta, 0.5), 2.0);
    utterance.volume = selected.volume;
    utterance.onstart = () =>
      setSpeakStatus(`${reasonPrefix} Memutar Android/Browser TTS Offline (${selected.characterName} · ${selectedEmotion.label}).`);
    utterance.onend = () => setSpeakStatus(`${reasonPrefix} Selesai.`);
    utterance.onerror = () => setSpeakStatus(`${reasonPrefix} Pemutaran suara browser tidak didukung.`);
    window.speechSynthesis.speak(utterance);
  };

  const handleTestVoice = async () => {
    if (activeAudioRef.current) {
      activeAudioRef.current.pause();
      activeAudioRef.current = null;
    }

    const useOnlineGemini =
      simulatedOnline &&
      (operatingMode === 'AUTOMATIC' ? selected.voiceProvider !== 'ANDROID_TTS' : selected.voiceProvider === 'GEMINI_TTS');

    if (!useOnlineGemini) {
      playOfflineFallback(simulatedOnline ? 'Mode Offline TTS:' : 'Offline Auto-Switch:');
      return;
    }

    const cacheKey = [
      geminiModel,
      selected.voiceId,
      selected.personalityId,
      selected.emotionId,
      selected.customDescription.trim(),
      selected.pitch.toFixed(2),
      selected.speechRate.toFixed(2),
      sampleDialogue.trim(),
    ].join('|');

    const cachedSrc = audioCacheRef.current.get(cacheKey);
    if (cachedSrc) {
      const audio = new Audio(cachedSrc);
      audio.volume = selected.volume;
      activeAudioRef.current = audio;
      await audio.play();
      setSpeakStatus(`Memutar dari Audio Cache (${selected.characterName} · ${selectedEmotion.label}) tanpa generate ulang.`);
      return;
    }

    setIsSynthesizing(true);
    setSpeakStatus(`Menghasilkan suara AI ekspresif (${geminiModel} · ${selected.voiceId})...`);

    try {
      const res = await fetch('/api/gemini-tts', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          text: sampleDialogue,
          stylePrompt: composedStylePrompt,
          voiceName: selected.voiceId || selectedPersonality.defaultVoice,
          model: geminiModel,
        }),
      });

      const data = (await res.json()) as { audioBase64?: string; mimeType?: string; error?: string };
      if (!res.ok || !data.audioBase64) {
        playOfflineFallback(`Fallback Otomatis (${data.error ?? `HTTP ${res.status}`}):`);
        return;
      }

      const dataUrl = `data:${data.mimeType ?? 'audio/wav'};base64,${data.audioBase64}`;
      audioCacheRef.current.set(cacheKey, dataUrl);
      const audio = new Audio(dataUrl);
      audio.volume = selected.volume;
      activeAudioRef.current = audio;
      await audio.play();
      setSpeakStatus(`Memutar Gemini Expressive TTS (${selected.voiceId} · ${selectedEmotion.label}) · Disimpan ke cache.`);
    } catch {
      playOfflineFallback('Koneksi gagal · Fallback Otomatis:');
    } finally {
      setIsSynthesizing(false);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold text-[#F8FAFC]">
            Dual Voice Engine &amp; AI Expressive Voice Studio
          </h2>
          <p className="text-sm text-[#94A3B8]">
            Uji Voice Personality bergaya anime, 8 Emotion Control, Custom Voice Description, Audio Cache, dan Auto-Switching Online ↔ Offline.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-2">
          <button
            type="button"
            onClick={() => setSimulatedOnline((v) => !v)}
            className={`px-3 py-2 rounded-lg text-xs font-medium border inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer ${
              simulatedOnline
                ? 'bg-[#161F33] border-[#22C55E] text-[#22C55E]'
                : 'bg-[#161F33] border-[#FACC15] text-[#FACC15]'
            }`}
          >
            {simulatedOnline ? <Wifi className="w-3.5 h-3.5" /> : <WifiOff className="w-3.5 h-3.5" />}
            <span>{simulatedOnline ? 'Simulasi: Online' : 'Simulasi: Offline'}</span>
          </button>

          <div className="flex items-center gap-1 p-1 bg-[#111827] border border-[#24304A] rounded-lg">
            <button
              type="button"
              onClick={() => setOperatingMode('AUTOMATIC')}
              className={`px-3 py-1 text-xs font-medium rounded-md cursor-pointer ${
                operatingMode === 'AUTOMATIC' ? 'bg-[#00E5FF] text-[#0A0F1E]' : 'text-[#94A3B8]'
              }`}
            >
              Automatic
            </button>
            <button
              type="button"
              onClick={() => setOperatingMode('MANUAL')}
              className={`px-3 py-1 text-xs font-medium rounded-md cursor-pointer ${
                operatingMode === 'MANUAL' ? 'bg-[#00E5FF] text-[#0A0F1E]' : 'text-[#94A3B8]'
              }`}
            >
              Manual
            </button>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
        <div className="lg:col-span-4 space-y-4">
          <div className="bg-[#111827] border border-[#24304A] rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between gap-2">
              <h3 className="text-sm font-semibold text-[#F8FAFC]">Profil Karakter</h3>
              <span className="font-mono tabular-nums text-[11px] text-[#94A3B8]">
                {characters.length} profil
              </span>
            </div>

            <div className="flex items-center gap-2">
              <input
                type="text"
                value={newCharName}
                onChange={(e) => setNewCharName(e.target.value)}
                placeholder="Karakter baru (mis. LUCY)"
                className="flex-1 min-w-0 px-3 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-xs text-[#F8FAFC] placeholder-[#64748B] focus:outline-none focus:border-[#00E5FF]"
              />
              <button
                type="button"
                onClick={addCharacter}
                className="px-3 py-2 rounded-lg bg-[#00E5FF] text-[#0A0F1E] text-xs font-semibold hover:bg-[#38EDFF] inline-flex items-center gap-1 whitespace-nowrap shrink-0 cursor-pointer"
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Tambah</span>
              </button>
            </div>

            <div className="space-y-2">
              {characters.map((char) => {
                const active = char.id === selected.id;
                const pName =
                  PERSONALITIES.find((p) => p.id === char.personalityId)?.label.split(' (')[0] ?? char.personalityId;
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
                      <div className="text-[11px] font-mono text-[#00E5FF] truncate">
                        {pName} · {char.emotionId}
                      </div>
                      <div className="text-[11px] font-mono text-[#94A3B8] truncate">
                        {char.voiceProvider} ({char.voiceId})
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
          </div>

          <div className="bg-[#111827] border border-[#24304A] rounded-xl p-4 space-y-2 text-xs text-[#94A3B8]">
            <h3 className="text-sm font-semibold text-[#F8FAFC]">Integrasi Gemini TTS, Kuota &amp; Biaya</h3>
            <p>
              Aplikasi Android menggunakan endpoint resmi Gemini Generative Language API (<code className="text-[#00E5FF]">gemini-3.8-flash-tts</code> / <code className="text-[#00E5FF]">gemini-3.8-flash-lite-tts</code>) tanpa API key hardcoded di APK.
            </p>
            <p>
              Free tier memiliki batas kuota RPM/RPD; bila kuota tercapai (HTTP 429) atau koneksi terputus, Dual Voice Engine otomatis beralih ke Android Native TTS secara gratis dan offline tanpa menghentikan OCR maupun ML Kit Translation.
            </p>
          </div>
        </div>

        <div className="lg:col-span-4 bg-[#111827] border border-[#24304A] rounded-xl p-5 space-y-4">
          <h3 className="text-sm font-semibold text-[#F8FAFC]">
            Voice Design: {selected.characterName}
          </h3>

          <div className="space-y-3 text-xs">
            <label className="block space-y-1">
              <span className="text-[#94A3B8]">Anime-Inspired Voice Personality</span>
              <select
                value={selected.personalityId}
                onChange={(e) => {
                  const p = PERSONALITIES.find((item) => item.id === e.target.value);
                  updateSelected({
                    personalityId: e.target.value,
                    voiceId:
                      selected.voiceProvider === 'GEMINI_TTS' && p
                        ? p.defaultVoice
                        : selected.voiceId,
                  });
                }}
                className="w-full px-3 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC]"
              >
                {PERSONALITIES.map((p) => (
                  <option key={p.id} value={p.id}>
                    {p.label}
                  </option>
                ))}
              </select>
            </label>

            <div className="space-y-1.5">
              <span className="text-[#94A3B8] block">Emotion Control</span>
              <div className="flex flex-wrap gap-1.5">
                {EMOTIONS.map((em) => (
                  <button
                    key={em.id}
                    type="button"
                    onClick={() => updateSelected({ emotionId: em.id })}
                    className={`px-2.5 py-1 rounded-md text-[11px] font-medium border cursor-pointer transition-colors ${
                      selected.emotionId === em.id
                        ? 'bg-[#00E5FF] text-[#0A0F1E] border-[#00E5FF]'
                        : 'bg-[#0A0F1E] text-[#CBD5E1] border-[#24304A] hover:border-[#334155]'
                    }`}
                  >
                    {em.label}
                  </button>
                ))}
              </div>
            </div>

            <label className="block space-y-1">
              <span className="text-[#94A3B8]">Custom Voice Description</span>
              <textarea
                rows={3}
                value={selected.customDescription}
                onChange={(e) => updateSelected({ customDescription: e.target.value })}
                placeholder="Suara perempuan dewasa bergaya anime Jepang, lembut, ceria, sedikit manja, dengan intonasi ekspresif..."
                className="w-full p-2.5 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-xs text-[#F8FAFC] placeholder-[#64748B] focus:outline-none focus:border-[#00E5FF]"
              />
            </label>

            <div className="grid grid-cols-2 gap-2">
              <label className="block space-y-1">
                <span className="text-[#94A3B8]">Provider</span>
                <select
                  value={selected.voiceProvider}
                  onChange={(e) =>
                    updateSelected({
                      voiceProvider: e.target.value as CharacterConfig['voiceProvider'],
                    })
                  }
                  className="w-full px-2.5 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC]"
                >
                  <option value="GEMINI_TTS">Gemini AI TTS</option>
                  <option value="EDGE_COMPATIBLE">Edge WS TTS</option>
                  <option value="ANDROID_TTS">Android Offline</option>
                </select>
              </label>

              <label className="block space-y-1">
                <span className="text-[#94A3B8]">Model Gemini</span>
                <select
                  value={geminiModel}
                  onChange={(e) =>
                    setGeminiModel(e.target.value as typeof geminiModel)
                  }
                  className="w-full px-2.5 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC]"
                >
                  <option value="gemini-3.8-flash-tts">flash-tts (Design)</option>
                  <option value="gemini-3.8-flash-lite-tts">flash-lite-tts</option>
                </select>
              </label>
            </div>

            <div className="grid grid-cols-2 gap-2">
              <label className="block space-y-1">
                <span className="text-[#94A3B8]">Voice ID Online</span>
                <input
                  type="text"
                  value={selected.voiceId}
                  onChange={(e) => updateSelected({ voiceId: e.target.value })}
                  className="w-full px-2.5 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC] font-mono"
                />
              </label>
              <label className="block space-y-1">
                <span className="text-[#94A3B8]">Offline Fallback Voice</span>
                <input
                  type="text"
                  value={selected.offlineVoiceId}
                  onChange={(e) => updateSelected({ offlineVoiceId: e.target.value })}
                  className="w-full px-2.5 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-[#F8FAFC] font-mono"
                />
              </label>
            </div>

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
            disabled={isSynthesizing}
            onClick={handleTestVoice}
            className="w-full py-2.5 px-4 rounded-lg bg-[#00E5FF] text-[#0A0F1E] text-xs font-semibold hover:bg-[#38EDFF] disabled:opacity-60 inline-flex items-center justify-center gap-2 cursor-pointer"
          >
            <Play className="w-3.5 h-3.5" />
            <span>{isSynthesizing ? 'Menghasilkan Audio AI...' : 'Test Voice (Dual Engine)'}</span>
          </button>

          {speakStatus && <p className="text-xs text-[#CBD5E1]">{speakStatus}</p>}
        </div>

        <div className="lg:col-span-4 space-y-4">
          <div className="bg-[#111827] border border-[#24304A] rounded-xl p-5 space-y-3">
            <h3 className="text-sm font-semibold text-[#F8FAFC]">Teks Uji Dialog</h3>
            <textarea
              rows={3}
              value={sampleDialogue}
              onChange={(e) => setSampleDialogue(e.target.value)}
              className="w-full p-2.5 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-xs text-[#F8FAFC] focus:outline-none focus:border-[#00E5FF]"
            />

            <h4 className="text-xs font-semibold text-[#F8FAFC]">
              Prompt Voice Design (Gemini TTS speechMetadata.style)
            </h4>
            <pre className="p-3 rounded-lg bg-[#070B14] border border-[#24304A] text-[11px] font-mono text-[#A855F7] whitespace-pre-wrap leading-relaxed">
              {composedStylePrompt}
            </pre>

            <h4 className="text-xs font-semibold text-[#F8FAFC]">
              Output SSMLGenerator (Provider WebSocket / XML)
            </h4>
            <pre className="p-3 rounded-lg bg-[#070B14] border border-[#24304A] text-[11px] font-mono text-[#00E5FF] overflow-x-auto leading-relaxed">
              {generatedSsml}
            </pre>
          </div>
        </div>
      </div>
    </div>
  );
}
