# Evidence Checklist

Capture screenshots from actual program runs. Keep the terminal output visible and readable; do not edit, synthesize, or substitute screenshots.

- [ ] `thread-lifecycle-output.png` — output showing all six states from `ThreadLifecycleDemo`.
- [ ] `race-condition-output.png` — expected and actual counts from `RaceConditionDemo`. A matching count is a valid run; do not alter the output to show a mismatch.
- [ ] `synchronization-output.png` — expected and actual counts, including the correct result from `SynchronizationSolution`.
- [ ] `volatile-demo-output.png` — output showing the worker observed the stop flag and terminated.
- [ ] `github-repository.png` — browser view of the uploaded repository and its assignment files, captured only after they are actually pushed.

## Capture on Windows

1. Open PowerShell in the `week_2-day_1` folder.
2. Run the relevant program with `java ClassName` and leave its output visible in the terminal.
3. Press `Windows+Shift+S`, select the terminal output, and save the image using the exact filename listed above in this `evidence` folder.
4. For `github-repository.png`, open the repository in a browser after upload and capture the visible repository contents.

Only mark an item complete after its real screenshot has been saved here. The checklist itself does not mean the screenshots exist.
