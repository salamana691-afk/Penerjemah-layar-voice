import { useEffect, useState } from 'react';
import { CodebaseExplorer, type AndroidFileEntry } from './components/CodebaseExplorer';
import { OverviewSection } from './components/OverviewSection';
import { RoiSimulator } from './components/RoiSimulator';
import { VoiceSsmlStudio } from './components/VoiceSsmlStudio';

type ActiveTab = 'overview' | 'roi' | 'voice' | 'codebase';

export default function App() {
  const [activeTab, setActiveTab] = useState<ActiveTab>('overview');
  const [files, setFiles] = useState<AndroidFileEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    async function fetchFiles() {
      try {
        setLoading(true);
        const res = await fetch('/api/android-files');
        if (!res.ok) {
          throw new Error(`HTTP ${res.status}`);
        }
        const data = (await res.json()) as { files: AndroidFileEntry[] };
        if (!cancelled) {
          setFiles(data.files ?? []);
          setLoadError(null);
        }
      } catch (err) {
        if (!cancelled) {
          setLoadError(
            err instanceof Error ? err.message : 'Gagal memuat berkas proyek Android.'
          );
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    }
    fetchFiles();
    return () => {
      cancelled = true;
    };
  }, []);

  const handleDownloadZip = () => {
    window.location.href = '/api/download-android-project';
  };

  return (
    <div className="min-h-screen bg-[#0A0F1E] text-[#F8FAFC] flex flex-col">
      <header className="sticky top-0 z-30 bg-[#0A0F1E]/95 backdrop-blur border-b border-[#24304A]">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 h-16 flex items-center justify-between gap-8">
          <a
            href="#overview"
            onClick={(e) => {
              e.preventDefault();
              setActiveTab('overview');
            }}
            className="text-lg font-bold tracking-tight text-[#F8FAFC] whitespace-nowrap shrink-0"
          >
            AutoDub Edge
          </a>

          <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
            <button
              type="button"
              onClick={() => setActiveTab('overview')}
              className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
                activeTab === 'overview'
                  ? 'text-[#00E5FF] border-b border-[#00E5FF]'
                  : 'text-[#94A3B8] hover:text-[#F8FAFC]'
              }`}
            >
              Overview
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('roi')}
              className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
                activeTab === 'roi'
                  ? 'text-[#00E5FF] border-b border-[#00E5FF]'
                  : 'text-[#94A3B8] hover:text-[#F8FAFC]'
              }`}
            >
              Dual-Box ROI
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('voice')}
              className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
                activeTab === 'voice'
                  ? 'text-[#00E5FF] border-b border-[#00E5FF]'
                  : 'text-[#94A3B8] hover:text-[#F8FAFC]'
              }`}
            >
              Voice &amp; SSML
            </button>
            <button
              type="button"
              onClick={() => setActiveTab('codebase')}
              className={`transition-colors whitespace-nowrap shrink-0 cursor-pointer pb-0.5 ${
                activeTab === 'codebase'
                  ? 'text-[#00E5FF] border-b border-[#00E5FF]'
                  : 'text-[#94A3B8] hover:text-[#F8FAFC]'
              }`}
            >
              Codebase
            </button>
          </nav>

          <div className="shrink-0">
            <button
              type="button"
              onClick={handleDownloadZip}
              className="px-4 py-2 rounded-lg bg-[#00E5FF] text-[#0A0F1E] text-xs font-semibold hover:bg-[#38EDFF] transition-colors whitespace-nowrap shrink-0 cursor-pointer"
            >
              Export Android ZIP
            </button>
          </div>
        </div>

        <div className="md:hidden border-t border-[#24304A] px-4 py-2 flex items-center gap-4 overflow-x-auto text-xs font-medium">
          {(
            [
              ['overview', 'Overview'],
              ['roi', 'Dual-Box ROI'],
              ['voice', 'Voice & SSML'],
              ['codebase', 'Codebase'],
            ] as const
          ).map(([id, label]) => (
            <button
              key={id}
              type="button"
              onClick={() => setActiveTab(id)}
              className={`whitespace-nowrap shrink-0 py-1 cursor-pointer ${
                activeTab === id ? 'text-[#00E5FF]' : 'text-[#94A3B8]'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      </header>

      <main className="flex-1 max-w-7xl w-full mx-auto px-4 sm:px-6 py-8">
        {activeTab === 'overview' && (
          <OverviewSection
            fileCount={files.length}
            onNavigateTab={(tab) => setActiveTab(tab)}
            onDownloadZip={handleDownloadZip}
          />
        )}
        {activeTab === 'roi' && <RoiSimulator />}
        {activeTab === 'voice' && <VoiceSsmlStudio />}
        {activeTab === 'codebase' && (
          <CodebaseExplorer
            files={files}
            loading={loading}
            error={loadError}
            onDownloadZip={handleDownloadZip}
          />
        )}
      </main>

      <footer className="border-t border-[#24304A] py-5 px-4 sm:px-6">
        <div className="max-w-7xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-2 text-xs text-[#64748B]">
          <span>AutoDub Edge · com.autodub.edge · Android 8.0+ (API 26–35)</span>
          <span>Kotlin · Jetpack Compose · ML Kit · Room · Media3 ExoPlayer</span>
        </div>
      </footer>
    </div>
  );
}
