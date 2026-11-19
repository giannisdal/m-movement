import random
import sys


def move() -> None:
    # Small random movement (between -20 and 20 pixels)
    dx: int = random.randint(-20, 20)
    dy: int = random.randint(-20, 20)

    if sys.platform == "win32":
        _move_windows(dx, dy)
    else:
        _move_linux(dx, dy)


def _move_windows(dx: int, dy: int) -> None:
    import ctypes

    class POINT(ctypes.Structure):
        _fields_ = [("x", ctypes.c_long), ("y", ctypes.c_long)]

    point = POINT()
    ctypes.windll.user32.GetCursorPos(ctypes.byref(point))
    ctypes.windll.user32.SetCursorPos(point.x + dx, point.y + dy)


def _move_linux(dx: int, dy: int) -> None:
    import pyautogui

    pyautogui.moveRel(dx, dy, duration=0.3)


