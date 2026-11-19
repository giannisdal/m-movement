"""
Run the mouse mover natively on Windows.
Usage:  python run_windows.py
"""
import time
import logging
from apscheduler.schedulers.background import BackgroundScheduler
from movement import move

logging.basicConfig(level=logging.INFO, format="%(asctime)s  %(levelname)s  %(message)s")
logger = logging.getLogger(__name__)

if __name__ == "__main__":
    scheduler = BackgroundScheduler()
    scheduler.add_job(move, trigger="interval", seconds=20, id="mouse_move", replace_existing=True)
    scheduler.start()
    logger.info("Mouse mover started. Press Ctrl+C to stop.")
    try:
        while True:
            time.sleep(1)
    except (KeyboardInterrupt, SystemExit):
        scheduler.shutdown()
        logger.info("Stopped.")

