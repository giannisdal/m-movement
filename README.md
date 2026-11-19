# movement

A lightweight background utility that nudges your mouse cursor every 20 seconds to prevent your screen from locking or going idle.

---

## How it works

A scheduler fires every 20 seconds and moves the cursor by a small random offset (−20 to +20 pixels in each axis) using the native Windows API (`ctypes`). No third-party input library is needed on Windows.

---

## Requirements

- Python 3.10+
- [`apscheduler`](https://pypi.org/project/APScheduler/)

Install dependencies:

```bash
pip install -r requirements.txt
```

---

## Usage

### Run manually

```powershell
python run_windows.py
```

The script runs silently in the background and logs each move to the console.

### Run without a console window

```powershell
Start-Process pythonw -ArgumentList "run_windows.py" -WorkingDirectory (Get-Location) -WindowStyle Hidden
```

### Stop it

```powershell
Stop-Process -Name pythonw
```

---

## Auto-start on login (Windows)

A shortcut is already placed in your Startup folder:

```
%APPDATA%\Microsoft\Windows\Start Menu\Programs\Startup\MouseMover.lnk
```

This launches `run_windows.py` silently via `pythonw` every time you log in. To disable auto-start, delete that shortcut.

---

## Project structure

```
movement/
├── run_windows.py   # Native Windows runner (use this)
├── movement.py      # Core move logic (Windows via ctypes, Linux via pyautogui)
├── main.py          # FastAPI + APScheduler app (Linux / Docker only)
├── Dockerfile       # Linux container build (not needed on Windows)
├── docker-compose.yml
├── entrypoint.sh    # Starts Xvfb + app inside the container
├── requirements.txt
└── log_config.py
```

---

## Platform notes

| Platform | How to run | Mouse control |
|----------|-----------|---------------|
| **Windows** | `python run_windows.py` | `ctypes` → `SetCursorPos` |
| **Linux / Docker** | `docker compose up` | `pyautogui` via Xvfb virtual display |

> **Note:** The Docker setup moves the mouse inside a virtual X display and has no effect on the host's screen. On Windows always use `run_windows.py`.

