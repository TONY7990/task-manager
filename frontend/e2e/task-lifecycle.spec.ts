import { test, expect } from '@playwright/test';

test.describe('Task Management & AI QA Testing Lifecycle', () => {

  test('Complete QA Flow: Create Task -> Generate AI Tests -> Approve -> PASS', async ({ page }) => {
    // 1. Visit Tasks page
    await page.goto('http://localhost:4200/tasks');
    await expect(page.locator('h2')).toContainText('Tasks');

    // 2. Open Add Task Form
    await page.click('button:has-text("+ Add Task")');
    await page.waitForURL('**/tasks/new');

    // 3. Fill Form with valid business rule values
    const testTitle = `Auto QA Task ${Date.now()}`;
    await page.fill('input[formControlName="title"]', testTitle);
    await page.fill('textarea[formControlName="description"]', 'E2E automated flow verification');
    await page.selectOption('select[formControlName="priority"]', 'HIGH');

    // Due date (2 days in the future to satisfy Rule 5)
    const futureDate = new Date();
    futureDate.setDate(futureDate.getDate() + 2);
    const dateStr = futureDate.toISOString().split('T')[0];
    await page.fill('input[formControlName="dueDate"]', dateStr);

    // 4. Save and return to list
    await page.click('button:has-text("Save")');
    await page.waitForURL('**/tasks');

    // 5. Locate newly created task and open details
    const row = page.locator(`tr:has-text("${testTitle}")`);
    await expect(row).toBeVisible();
    await row.locator('button:has-text("View")').click();
    await page.waitForURL(/.*\/tasks\/\d+/);

    // 6. Generate AI Test Cases (matches "Generate AI Test Cases", "Generate Test Cases", etc.)
    const genButton = page.getByRole('button', { name: /generate.*test/i });
    await expect(genButton).toBeVisible({ timeout: 10000 });
    await genButton.click();

    // 7. Verify test cases rendered (POSITIVE, NEGATIVE, BOUNDARY, etc.)
    const testCaseBadge = page.locator('span, td, div').filter({ hasText: /POSITIVE|NEGATIVE|BOUNDARY/i }).first();
    await expect(testCaseBadge).toBeVisible({ timeout: 10000 });

    // 8. Tester Review: Approve the first test case
    const approveBtn = page.getByRole('button', { name: /approve/i }).first();
    await expect(approveBtn).toBeVisible();
    await approveBtn.click();

    // 9. Execute: Verify PASS button is unlocked and execute
    page.on('dialog', async dialog => {
      await dialog.accept('Automated execution confirmed working');
    });

    const passBtn = page.getByRole('button', { name: /pass/i }).first();
    await expect(passBtn).toBeVisible();
    await passBtn.click();

    // 10. Verify execution state updated
    await expect(page.locator('text=/APPROVED|PASS/i').first()).toBeVisible();
  });

});