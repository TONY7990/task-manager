import { defineConfig } from '@playwright/test';

export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.spec.ts',
  timeout: 30000,
  use: {
    baseURL: 'http://localhost:4200',
    channel: 'chrome',
    headless: false,
    screenshot: 'only-on-failure',
    video: 'off'
  },
  projects: [
    {
      name: 'Google Chrome',
      use: { 
        channel: 'chrome',
        video: 'off'
      },
    },
  ],
});