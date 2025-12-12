import firebase_admin
from firebase_admin import credentials, firestore
import requests
import json
import os
import sys

# usage: python3 deploy_config.py [--rules] [--indexes]

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_PATH = os.path.join(BASE_DIR, "..", "service-account.json")
RULES_PATH = os.path.join(BASE_DIR, "..", "firestore.rules")
INDEXES_PATH = os.path.join(BASE_DIR, "..", "firestore.indexes.json")

def get_access_token():
    cred = credentials.Certificate(KEY_PATH)
    return cred.get_access_token().access_token

def get_project_id():
    with open(KEY_PATH) as f:
        return json.load(f)['project_id']

def deploy_firestore_rules(project_id, token):
    print("Deploying Firestore Rules...")
    try:
        with open(RULES_PATH, 'r') as f:
            rules_content = f.read()
            
        url = f"https://firebaserules.googleapis.com/v1/projects/{project_id}/rulesets"
        headers = {
            "Authorization": f"Bearer {token}",
            "Content-Type": "application/json"
        }
        
        # 1. Create Ruleset
        payload = {
            "source": {
                "files": [
                    {
                        "content": rules_content,
                        "name": "firestore.rules"
                    }
                ]
            }
        }
        
        response = requests.post(url, headers=headers, json=payload)
        if response.status_code != 200:
            print(f"Error creating ruleset: {response.text}")
            return
            
        ruleset_name = response.json()['name']
        print(f"- Created Ruleset: {ruleset_name}")
        
        # 2. Update Release
        release_url = f"https://firebaserules.googleapis.com/v1/projects/{project_id}/releases/cloud.firestore"
        release_payload = {
            "name": f"projects/{project_id}/releases/cloud.firestore",
            "rulesetName": ruleset_name
        }
        
        # Try updating first (PATCH), if 404 then CREATE (POST) logic varies, usually UPDATE works for standard release
        response = requests.patch(release_url, headers=headers, json=release_payload)
        if response.status_code != 200:
             # Try Create?
             create_release_url = f"https://firebaserules.googleapis.com/v1/projects/{project_id}/releases"
             response = requests.post(create_release_url, headers=headers, json=release_payload)
        
        if response.status_code == 200:
            print("- Rules released successfully!")
        else:
            print(f"Error releasing rules: {response.text}")

    except Exception as e:
        print(f"FAILED to deploy rules: {e}")

def deploy_indexes(project_id, token):
    print("Deploying Indexes (Admin API hack)...")
    # Note: The Admin API for indexes is complex. 
    # Usually easier to print the link for the user or use CLI. 
    # But we can try to use the REST API: https://firestore.googleapis.com/v1/projects/{projectId}/databases/{databaseId}/collectionGroups/{collectionId}/indexes
    print("NOTE: Deploying indexes via raw REST API is brittle. Standard CLI is recommended.")
    print("Skipping auto-deploy for indexes to avoid breaking things. Please use CLI when available.")

if __name__ == "__main__":
    if not os.path.exists(KEY_PATH):
        print("Missing service-account.json")
        sys.exit(1)
        
    token = get_access_token()
    project_id = get_project_id()
    
    deploy_firestore_rules(project_id, token)
