import { useMemo, useState } from 'react';
import { Check, Copy, Download, FileCode2, Search } from 'lucide-react';

export interface AndroidFileEntry {
  path: string;
  content: string;
  sizeBytes: number;
  category: string;
}

interface CodebaseExplorerProps {
  files: AndroidFileEntry[];
  loading: boolean;
  error: string | null;
  onDownloadZip: () => void;
}

const CATEGORIES = [
  'Semua',
  'Gradle & Manifest',
  'Core & Domain',
  'Data & Room',
  'Engines',
  'Services & Overlay',
  'Compose UI',
  'Tests',
];

export function CodebaseExplorer({
  files,
  loading,
  error,
  onDownloadZip,
}: CodebaseExplorerProps) {
  const [selectedCategory, setSelectedCategory] = useState<string>('Semua');
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [selectedPath, setSelectedPath] = useState<string>('');
  const [copied, setCopied] = useState(false);

  const filteredFiles = useMemo(() => {
    const q = searchQuery.trim().toLowerCase();
    return files.filter((f) => {
      const catOk = selectedCategory === 'Semua' || f.category === selectedCategory;
      const searchOk = !q || f.path.toLowerCase().includes(q);
      return catOk && searchOk;
    });
  }, [files, selectedCategory, searchQuery]);

  const activeFile = useMemo(() => {
    if (filteredFiles.length === 0) return null;
    return filteredFiles.find((f) => f.path === selectedPath) ?? filteredFiles[0];
  }, [filteredFiles, selectedPath]);

  const handleCopy = async () => {
    if (!activeFile) return;
    await navigator.clipboard.writeText(activeFile.content);
    setCopied(true);
    setTimeout(() => setCopied(false), 1800);
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h2 className="text-xl font-semibold text-[#F8FAFC]">
            Android Studio Codebase Explorer
          </h2>
          <p className="text-sm text-[#94A3B8]">
            Seluruh file Kotlin, Jetpack Compose, Room, Gradle, AndroidManifest, dan Unit Test dibaca langsung dari proyek.
          </p>
        </div>

        <button
          type="button"
          onClick={onDownloadZip}
          className="px-4 py-2 rounded-lg bg-[#00E5FF] text-[#0A0F1E] text-xs font-semibold hover:bg-[#38EDFF] inline-flex items-center gap-2 whitespace-nowrap shrink-0 cursor-pointer self-start"
        >
          <Download className="w-3.5 h-3.5" />
          <span>Ekspor Proyek (.zip)</span>
        </button>
      </div>

      <div className="flex flex-wrap items-center gap-1.5">
        {CATEGORIES.map((cat) => (
          <button
            key={cat}
            type="button"
            onClick={() => setSelectedCategory(cat)}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium border transition-colors whitespace-nowrap shrink-0 cursor-pointer ${
              selectedCategory === cat
                ? 'bg-[#00E5FF] text-[#0A0F1E] border-[#00E5FF]'
                : 'bg-[#111827] text-[#94A3B8] border-[#24304A] hover:text-[#F8FAFC]'
            }`}
          >
            {cat}
          </button>
        ))}
      </div>

      {loading && (
        <div className="bg-[#111827] border border-[#24304A] rounded-xl p-8 text-center text-sm text-[#94A3B8]">
          Memuat daftar file proyek Android...
        </div>
      )}

      {error && (
        <div className="bg-[#111827] border border-[#EF4444]/50 rounded-xl p-6 text-sm text-[#FCA5A5]">
          {error}
        </div>
      )}

      {!loading && !error && (
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-4 items-stretch">
          <div className="lg:col-span-4 bg-[#111827] border border-[#24304A] rounded-xl flex flex-col overflow-hidden max-h-[680px]">
            <div className="p-3 border-b border-[#24304A]">
              <div className="relative">
                <Search className="w-4 h-4 text-[#64748B] absolute left-3 top-1/2 -translate-y-1/2" />
                <input
                  type="text"
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  placeholder="Cari file (mis. ScreenCapture, Room)..."
                  className="w-full pl-9 pr-3 py-2 rounded-lg bg-[#0A0F1E] border border-[#24304A] text-xs text-[#F8FAFC] placeholder-[#64748B] focus:outline-none focus:border-[#00E5FF]"
                />
              </div>
            </div>

            <div className="overflow-y-auto divide-y divide-[#24304A]/60">
              {filteredFiles.length === 0 ? (
                <div className="p-6 text-center text-xs text-[#94A3B8]">
                  Tidak ada file yang cocok dengan filter.
                </div>
              ) : (
                filteredFiles.map((file) => {
                  const isActive = activeFile?.path === file.path;
                  const shortName = file.path.split('/').pop() ?? file.path;
                  return (
                    <button
                      key={file.path}
                      type="button"
                      onClick={() => setSelectedPath(file.path)}
                      className={`w-full text-left px-3.5 py-2.5 transition-colors cursor-pointer ${
                        isActive ? 'bg-[#161F33]' : 'hover:bg-[#0A0F1E]/70'
                      }`}
                    >
                      <div className="flex items-center justify-between gap-2">
                        <span
                          className={`text-xs font-medium truncate ${
                            isActive ? 'text-[#00E5FF]' : 'text-[#F8FAFC]'
                          }`}
                        >
                          {shortName}
                        </span>
                        <span className="font-mono tabular-nums text-[10px] text-[#64748B] shrink-0">
                          {(file.sizeBytes / 1024).toFixed(1)} KB
                        </span>
                      </div>
                      <div className="text-[11px] font-mono text-[#64748B] truncate mt-0.5">
                        {file.path}
                      </div>
                    </button>
                  );
                })
              )}
            </div>
          </div>

          <div className="lg:col-span-8 bg-[#111827] border border-[#24304A] rounded-xl flex flex-col overflow-hidden max-h-[680px]">
            {activeFile ? (
              <>
                <div className="px-4 py-3 border-b border-[#24304A] flex items-center justify-between gap-4">
                  <div className="flex items-center gap-2 min-w-0">
                    <FileCode2 className="w-4 h-4 text-[#00E5FF] shrink-0" />
                    <span className="font-mono text-xs text-[#F8FAFC] truncate">
                      {activeFile.path}
                    </span>
                  </div>

                  <button
                    type="button"
                    onClick={handleCopy}
                    className="px-3 py-1.5 rounded-lg bg-[#161F33] border border-[#24304A] text-xs text-[#E2E8F0] hover:border-[#00E5FF]/50 inline-flex items-center gap-1.5 whitespace-nowrap shrink-0 cursor-pointer"
                  >
                    {copied ? (
                      <>
                        <Check className="w-3.5 h-3.5 text-[#22C55E]" />
                        <span>Disalin</span>
                      </>
                    ) : (
                      <>
                        <Copy className="w-3.5 h-3.5" />
                        <span>Salin Kode</span>
                      </>
                    )}
                  </button>
                </div>

                <pre className="p-4 text-xs font-mono text-[#E2E8F0] overflow-auto leading-relaxed bg-[#070B14] flex-1">
                  <code>{activeFile.content}</code>
                </pre>
              </>
            ) : (
              <div className="p-8 text-center text-sm text-[#94A3B8]">
                Pilih file di sebelah kiri untuk melihat implementasi lengkapnya.
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}
