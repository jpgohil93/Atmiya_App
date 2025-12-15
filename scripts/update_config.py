import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import os

# Initialize Firebase Admin
# Script is in scripts/, Key is in root/
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_PATH = os.path.join(BASE_DIR, "..", "service-account.json")

print(f"Looking for key at: {KEY_PATH}")
if os.path.exists(KEY_PATH):
    cred = credentials.Certificate(KEY_PATH)
    firebase_admin.initialize_app(cred)
else:
    print(f"ERROR: Service account key not found at {KEY_PATH}")
    exit(1)
    

db = firestore.client()

def update_min_version(version_code):
    print(f"Updating min_version_code to {version_code}...")
    doc_ref = db.collection('app_config').document('android')
    
    doc_ref.set({
        'min_version_code': version_code,
        'force_update_title': 'Update Required',
        'force_update_message': 'A new version is available. Please update to continue.'
    }, merge=True)
    
    print("Update successful!")

if __name__ == "__main__":
    update_min_version(13)
