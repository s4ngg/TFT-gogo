import { readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { spawnSync } from 'node:child_process';

const testFiles = [];

function collectTestFiles(directory) {
  for (const entry of readdirSync(directory)) {
    const fullPath = join(directory, entry);
    const stats = statSync(fullPath);

    if (stats.isDirectory()) {
      collectTestFiles(fullPath);
      continue;
    }

    if (/\.test\.tsx?$/.test(entry)) {
      testFiles.push(relative(process.cwd(), fullPath));
    }
  }
}

collectTestFiles(join(process.cwd(), 'src'));
testFiles.sort();

if (testFiles.length === 0) {
  console.error('No frontend test files found.');
  process.exit(1);
}

const command = process.platform === 'win32' ? 'tsx.cmd' : 'tsx';
const result = spawnSync(command, ['--test', '--test-concurrency=1', ...testFiles], {
  stdio: 'inherit',
  shell: process.platform === 'win32',
});

if (result.error) {
  console.error(result.error.message);
  process.exit(1);
}

process.exit(result.status ?? 1);
