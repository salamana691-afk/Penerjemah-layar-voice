import { ArrowRight, CheckCircle2, Layers, ShieldCheck } from 'lucide-react';

interface OverviewSectionProps {
  fileCount: number;
  onNavigateTab: (tab: 'roi' | 'voice' | 'codebase') => void;
  onDownloadZip: () => void;
}

const PIPELINE_STEPS = [
  {
    step: '01. Screen Capture',
    component: 'ScreenCaptureService',
    detail: 'MediaProjection + VirtualDisplay + ImageReader RGBA_8888 dengan throttle interval dan penyesuaian rotasi layar otomatis.',
  },
  {
    step: '02. Dual-Box ROI Crop',
    component: 'DualBoxOcrProcessor',
    detail: 'Memotong Bitmap hanya pada ROI 1 (Nama Karakter) & ROI 2 (Dialog) sebelum memanggil ML Kit Text Recognition.',
  },
  {
    step: '03. Normalization & Match',
    component: 'CharacterMatcher',
    detail: 'Normalisasi nama, toleransi typo OCR konservatif (Levenshtein >= 0.80), dan deteksi duplikasi berbasis identitas + dialog.',
  },
  {
    step: '04. Offline Translation',
    component: 'MlKitTranslationRepository',
    detail: 'Google ML Kit Translation EN → ID dengan unduh model otomatis, cache LRU 250 entri, dan mode offline penuh.',
  },
  {
    step: '05. SSML & Voice Engine',
    component: 'VoiceOrchestrator',
    detail: 'Menghasilkan SSML tervalidasi (anti-XSS/XML injection) untuk Edge-Compatible WS TTS dengan fallback otomatis Android TextToSpeech.',
  },
  {
    step: '06. Audio & Overlay',
    component: 'AudioPlaybackManager',
    detail: 'Media3 ExoPlayer memutar dubbing sesuai kebijakan antrean (Replace / Finish / Queue All) bersamaan dengan Floating Translation Overlay.',
  },
];

const ACCEPTANCE_ITEMS = [
  'Minimum Android 8.0 (API 26), targetSdk 35, AGP 8.7.3 & Kotlin 2.0.21',
  'Room Database v2 dengan MIGRATION_1_2 dan profil karakter permanen',
  'DataStore Preferences untuk tata letak Dual-Box ROI dan tampilan overlay',
  'Izin runtime POST_NOTIFICATIONS, SYSTEM_ALERT_WINDOW, dan MediaProjection',
  'Penanganan error terstruktur via AppResult & AppError tanpa crash',
  'Unit test & instrumentation test untuk normalizer, matcher, SSML, dan Room',
];

export function OverviewSection({
  fileCount,
  onNavigateTab,
  onDownloadZip,
}: OverviewSectionProps) {
  return (
    <div className="space-y-10">
      <section className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-stretch">
        <div className="lg:col-span-7 bg-[#111827] border border-[#24304A] rounded-xl p-6 sm:p-8 flex flex-col justify-between gap-6">
          <div className="space-y-4">
            <div className="flex flex-wrap items-center gap-2 text-xs text-[#94A3B8]">
              <span>com.autodub.edge</span>
              <span aria-hidden="true">·</span>
              <span>Kotlin &amp; Jetpack Compose</span>
              <span aria-hidden="true">·</span>
              <span className="font-mono tabular-nums">{fileCount} project files</span>
            </div>

            <h1 className="text-3xl sm:text-4xl font-bold tracking-tight text-[#F8FAFC] text-balance">
              Penerjemah Layar Visual Novel &amp; Multi-Character AI Voice Dubber
            </h1>

            <p className="text-sm sm:text-base text-[#94A3B8] leading-relaxed max-w-2xl">
              Proyek Android lengkap dengan arsitektur MVVM + Repository Pattern, Dual-Box OCR berbasis ML Kit, penerjemahan offline Inggris ke Indonesia, floating overlay non-blocking, serta mesin suara multi-karakter berbasis Room Database dan Media3 ExoPlayer.
            </p>
          </div>

          <div className="flex flex-wrap items-center gap-3 pt-2">
            <button
              type="button"
              onClick={onDownloadZip}
              className="px-5 py-2.5 rounded-lg bg-[#00E5FF] text-[#0A0F1E] text-sm font-semibold hover:bg-[#38EDFF] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              Unduh Proyek Android (.zip)
            </button>
            <button
              type="button"
              onClick={() => onNavigateTab('codebase')}
              className="px-4 py-2.5 rounded-lg bg-[#161F33] border border-[#24304A] text-[#F8FAFC] text-sm font-medium hover:border-[#00E5FF]/50 transition-colors whitespace-nowrap shrink-0 cursor-pointer inline-flex items-center gap-2"
            >
              <span>Jelajahi Kode Sumber</span>
              <ArrowRight className="w-4 h-4 text-[#00E5FF]" />
            </button>
            <button
              type="button"
              onClick={() => onNavigateTab('roi')}
              className="px-4 py-2.5 rounded-lg bg-transparent border border-[#24304A] text-[#CBD5E1] text-sm font-medium hover:text-[#F8FAFC] hover:border-[#334155] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              Simulasi Dual-Box ROI
            </button>
          </div>
        </div>

        <div className="lg:col-span-5 bg-[#111827] border border-[#24304A] rounded-xl p-6 flex flex-col justify-between gap-4">
          <div className="space-y-1">
            <div className="flex items-center gap-2 text-sm font-semibold text-[#F8FAFC]">
              <Layers className="w-4 h-4 text-[#00E5FF]" />
              <span>Spesifikasi Teknis Target APK</span>
            </div>
            <p className="text-xs text-[#94A3B8]">
              Siap dibuka di Android Studio Ladybug / Meerkat ke atas.
            </p>
          </div>

          <div className="divide-y divide-[#24304A] text-sm">
            <div className="py-2.5 flex items-center justify-between gap-4">
              <span className="text-[#94A3B8]">Package Name</span>
              <span className="font-mono text-xs text-[#F8FAFC]">com.autodub.edge</span>
            </div>
            <div className="py-2.5 flex items-center justify-between gap-4">
              <span className="text-[#94A3B8]">SDK Range</span>
              <span className="font-mono tabular-nums text-xs text-[#00E5FF]">minSdk 26 · targetSdk 35</span>
            </div>
            <div className="py-2.5 flex items-center justify-between gap-4">
              <span className="text-[#94A3B8]">Database &amp; Prefs</span>
              <span className="font-mono text-xs text-[#F8FAFC]">Room 2.6.1 · DataStore 1.1.1</span>
            </div>
            <div className="py-2.5 flex items-center justify-between gap-4">
              <span className="text-[#94A3B8]">Vision &amp; Translate</span>
              <span className="font-mono text-xs text-[#F8FAFC]">ML Kit OCR 16.0.1 · Translate 17.0.3</span>
            </div>
            <div className="py-2.5 flex items-center justify-between gap-4">
              <span className="text-[#94A3B8]">Audio &amp; Streaming</span>
              <span className="font-mono text-xs text-[#F8FAFC]">Media3 ExoPlayer 1.5.1 · OkHttp 4.12</span>
            </div>
          </div>
        </div>
      </section>

      <section className="space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-2">
          <div>
            <h2 className="text-xl font-semibold text-[#F8FAFC]">Aliran Data Real-Time</h2>
            <p className="text-sm text-[#94A3B8]">
              Dari frame tangkapan layar MediaProjection hingga pemutaran suara multi-karakter.
            </p>
          </div>
          <button
            type="button"
            onClick={() => onNavigateTab('voice')}
            className="text-xs font-medium text-[#00E5FF] hover:underline whitespace-nowrap shrink-0 cursor-pointer"
          >
            Uji Profil Suara &amp; SSML Generator →
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {PIPELINE_STEPS.map((item) => (
            <div
              key={item.step}
              className="bg-[#111827] border border-[#24304A] rounded-xl p-5 space-y-2.5"
            >
              <div className="flex items-center justify-between gap-2">
                <span className="text-sm font-semibold text-[#F8FAFC]">{item.step}</span>
                <span className="font-mono text-[11px] text-[#00E5FF] truncate">{item.component}</span>
              </div>
              <p className="text-xs text-[#94A3B8] leading-relaxed">{item.detail}</p>
            </div>
          ))}
        </div>
      </section>

      <section className="bg-[#111827] border border-[#24304A] rounded-xl p-6 space-y-4">
        <div className="flex items-center gap-2 text-base font-semibold text-[#F8FAFC]">
          <ShieldCheck className="w-5 h-5 text-[#22C55E]" />
          <span>Checklist Kesiapan Kompilasi Android Studio</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-3">
          {ACCEPTANCE_ITEMS.map((item) => (
            <div
              key={item}
              className="flex items-start gap-2.5 text-xs sm:text-sm text-[#CBD5E1]"
            >
              <CheckCircle2 className="w-4 h-4 text-[#22C55E] shrink-0 mt-0.5" />
              <span>{item}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
