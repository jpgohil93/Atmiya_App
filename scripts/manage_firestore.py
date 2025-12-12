import firebase_admin
from firebase_admin import credentials
from firebase_admin import firestore
import sys
import os

# usage: python3 manage_firestore.py [action] [args]

# Script is in scripts/, Key is in root/
BASE_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_PATH = os.path.join(BASE_DIR, "..", "service-account.json")

def init():
    if not os.path.exists(KEY_PATH):
        print(f"ERROR: Missing service account key at {KEY_PATH}")
        print("1. Go to Firebase Console -> Project Settings -> Service Accounts")
        print("2. Generate New Private Key")
        print("3. Save as 'service-account.json' in the project root")
        sys.exit(1)

    cred = credentials.Certificate(KEY_PATH)
    firebase_admin.initialize_app(cred)
    return firestore.client()

def list_users(db, role=None):
    users_ref = db.collection('users')
    if role:
        users_ref = users_ref.where('role', '==', role)
    
    docs = users_ref.stream()
    print(f"{'UID':<30} | {'Name':<20} | {'Role':<10} | {'Status'}")
    print("-" * 80)
    for doc in docs:
        d = doc.to_dict()
        status = []
        if d.get('isDeleted'): status.append('DELETED')
        if d.get('isBlocked'): status.append('BLOCKED')
        if not status: status.append('ACTIVE')
        
        print(f"{doc.id:<30} | {d.get('name', 'N/A'):<20} | {d.get('role', 'N/A'):<10} | {', '.join(status)}")

def delete_user_completely(db, uid):
    # 1. Get User to find role
    user_doc = db.collection('users').document(uid).get()
    if not user_doc.exists:
        print("User not found!")
        return
    
    user_data = user_doc.to_dict()
    role = user_data.get('role')
    
    print(f"Deleting user {uid} ({user_data.get('name')})...")
    
    # 2. Delete from Users
    db.collection('users').document(uid).delete()
    print("- Deleted from 'users'")
    
    # 3. Delete from Role Collection
    if role:
        db.collection(f"{role}s").document(uid).delete()
        print(f"- Deleted from '{role}s'")
    
    print("Done. (Note: Auth account still exists, use Firebase Console to remove login)")

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Usage: python3 manage_firestore.py [list_users|delete_user] [args]")
        sys.exit(1)
        
    db = init()
    command = sys.argv[1]
    
    if command == "list_users":
        role = sys.argv[2] if len(sys.argv) > 2 else None
        list_users(db, role)
    elif command == "delete_user":
        if len(sys.argv) < 3:
            print("Usage: python3 manage_firestore.py delete_user [uid]")
        else:
            delete_user_completely(db, sys.argv[2])
    else:
        print("Unknown command")
