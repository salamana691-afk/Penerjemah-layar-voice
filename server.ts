import express from 'express';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import JSZip from 'jszip';
import { GoogleGenAI } from '@google/genai';
import { createServer as createViteServer } from 'vite';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

interface AndroidProjectFile {
  path: string;
  content: string;
  sizeBytes: number;
  category: string;
}

const ANDROID_ROOT_FILES = [
  'settings.gradle.kts',
  'build.gradle.kts',
  'gradle.properties',
];

const ANDROID_ROOT_DIRS = [
  'gradle',
  'app',
];

function classifyFile(relPath: string): string {
  if (
    relPath.endsWith('.gradle.kts') ||
    relPath.endsWith('.properties') ||
    relPath.endsWith('.toml') ||
    relPath.endsWith('.pro') ||
    relPath.endsWith('AndroidManifest.xml') ||
    relPath.includes('/res/')
  ) {
    return 'Gradle & Manifest';
  }
  if (relPath.includes('/src/test/') || relPath.includes('/src/androidTest/')) {
    return 'Tests';
  }
  if (relPath.includes('/core/') || relPath.includes('/domain/')) {
    return 'Core & Domain';
  }
  if (relPath.includes('/data/')) {
    return 'Data & Room';
  }
  if (relPath.includes('/engine/')) {
    return 'Engines';
  }
  if (relPath.includes('/service/') || relPath.includes('/overlay/')) {
    return 'Services & Overlay';
  }
  return 'Compose UI';
}

function collectFilesRecursively(dirAbs: string, baseAbs: string, out: AndroidProjectFile[]) {
  if (!fs.existsSync(dirAbs)) return;
  const entries = fs.readdirSync(dirAbs, { withFileTypes: true });
  for (const entry of entries) {
    const full = path.join(dirAbs, entry.name);
    if (entry.isDirectory()) {
      if (entry.name === 'build' || entry.name === '.gradle') continue;
      collectFilesRecursively(full, baseAbs, out);
    } else if (entry.isFile()) {
      const rel = path.relative(baseAbs, full).split(path.sep).join('/');
      const content = fs.readFileSync(full, 'utf8');
      out.push({
        path: rel,
        content,
        sizeBytes: Buffer.byteLength(content, 'utf8'),
        category: classifyFile(rel),
      });
    }
  }
}

function loadAndroidProjectFiles(): AndroidProjectFile[] {
  const files: AndroidProjectFile[] = [];
  for (const rootFile of ANDROID_ROOT_FILES) {
    const abs = path.join(__dirname, rootFile);
    if (fs.existsSync(abs)) {
      const content = fs.readFileSync(abs, 'utf8');
      files.push({
        path: rootFile,
        content,
        sizeBytes: Buffer.byteLength(content, 'utf8'),
        category: classifyFile(rootFile),
      });
    }
  }
  for (const rootDir of ANDROID_ROOT_DIRS) {
    collectFilesRecursively(path.join(__dirname, rootDir), __dirname, files);
  }
  return files.sort((a, b) => a.path.localeCompare(b.path));
}

function getGeminiClient(): GoogleGenAI | null {
  const key = process.env.GEMINI_API_KEY?.trim();
  if (!key) return null;
  return new GoogleGenAI({
    apiKey: key,
    httpOptions: {
      headers: {
        'User-Agent': 'aistudio-build',
      },
    },
  });
}

async function startServer() {
  const app = express();
  app.use(express.json({ limit: '1mb' }));
  const port = 3000;

  app.get('/api/android-files', (_req, res) => {
    try {
      const files = loadAndroidProjectFiles();
      res.json({
        projectName: 'AutoDubEdge',
        packageName: 'com.autodub.edge',
        fileCount: files.length,
        files,
      });
    } catch (error) {
      res.status(500).json({
        error: error instanceof Error ? error.message : 'Failed to enumerate Android project files',
      });
    }
  });

  app.get('/api/download-android-project', async (_req, res) => {
    try {
      const files = loadAndroidProjectFiles();
      const zip = new JSZip();
      const rootFolder = zip.folder('AutoDubEdge');
      for (const file of files) {
        rootFolder?.file(file.path, file.content);
      }
      const buffer = await zip.generateAsync({
        type: 'nodebuffer',
        compression: 'DEFLATE',
        compressionOptions: { level: 6 },
      });

      res.setHeader('Content-Type', 'application/zip');
      res.setHeader('Content-Disposition', 'attachment; filename="AutoDubEdge-Android.zip"');
      res.send(buffer);
    } catch (error) {
      res.status(500).json({
        error: error instanceof Error ? error.message : 'Failed to build ZIP archive',
      });
    }
  });

  app.post('/api/gemini-tts', async (req, res) => {
    const ai = getGeminiClient();
    if (!ai) {
      res.status(503).json({
        error: 'GEMINI_API_KEY belum dikonfigurasi di server. Sistem otomatis memakai fallback offline.',
      });
      return;
    }

    const { text, stylePrompt, voiceName, model } = req.body as {
      text?: string;
      stylePrompt?: string;
      voiceName?: string;
      model?: string;
    };

    const cleanText = text?.trim() ?? '';
    if (!cleanText) {
      res.status(400).json({ error: 'Teks dialog tidak boleh kosong.' });
      return;
    }

    const targetModel =
      model === 'gemini-3.8-flash-lite-tts' ? 'gemini-3.8-flash-lite-tts' : 'gemini-3.8-flash-tts';
    const targetVoice = voiceName?.trim() || 'Kore';

    try {
      const response = await ai.models.generateContent({
        model: targetModel,
        contents: [
          {
            role: 'user',
            parts: [
              {
                text: cleanText,
                ...(stylePrompt?.trim()
                  ? {
                      speechMetadata: {
                        style: stylePrompt.trim(),
                      },
                    }
                  : {}),
              },
            ],
          },
        ],
        config: {
          responseModalities: ['AUDIO'],
          speechConfig: {
            voiceConfig: {
              prebuiltVoiceConfig: {
                voiceName: targetVoice,
              },
            },
          },
        },
      });

      const parts = response.candidates?.[0]?.content?.parts ?? [];
      let audioBase64: string | undefined;
      for (const p of parts) {
        if (p.inlineData?.data) {
          audioBase64 = p.inlineData.data;
          break;
        }
      }

      if (!audioBase64) {
        res.status(502).json({
          error: 'Gemini TTS tidak mengembalikan data audio WAV.',
        });
        return;
      }

      res.json({
        audioBase64,
        mimeType: 'audio/wav',
        model: targetModel,
        voiceName: targetVoice,
      });
    } catch (error) {
      const message = error instanceof Error ? error.message : 'Gagal menghubungi Gemini TTS API.';
      res.status(502).json({ error: message });
    }
  });

  if (process.env.NODE_ENV === 'production') {
    const distDir = path.resolve(__dirname, 'dist');
    app.use(express.static(distDir));
    app.get('*', (_req, res) => {
      res.sendFile(path.join(distDir, 'index.html'));
    });
  } else {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  }

  app.listen(port, '0.0.0.0', () => {
    console.log(`AutoDub Edge server running at http://localhost:${port}`);
  });
}

startServer();
