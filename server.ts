import express from 'express';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import JSZip from 'jszip';
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

async function startServer() {
  const app = express();
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
