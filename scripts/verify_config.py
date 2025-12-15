import firebase_admin
from firebase_admin import credentials, firestore
import os
import sys

# Setup matching update_config.py
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_PATH = os.path.join(BASE_DIR, "..", "service-account.json")

if os.path.exists(KEY_PATH):
    cred = credentials.Certificate(KEY_PATH)
    firebase_admin.initialize_app(cred)
else:
    print(f"ERROR: Key not found at {KEY_PATH}")
    sys.exit(1)

db = firestore.client()

def verify_config():
    print("Reading app_config/android...")
    doc_ref = db.collection('app_config').document('android')
    doc = doc_ref.get()
    
    if doc.exists:
        data = doc.to_dict()
        print(f"Document Found!")
        print(f"Data: {data}")
        min_ver = data.get('min_version_code')
        print(f"min_version_code: {min_ver} (Type: {type(min_ver)})")
    else:
        print("ERROR: Document app_config/android DOES NOT EXIST.")

if __name__ == "__main__":
    verify_config()
