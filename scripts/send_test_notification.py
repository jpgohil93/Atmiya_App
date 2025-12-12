import firebase_admin
from firebase_admin import credentials, messaging
import sys
import os

# usage: python3 send_test_notification.py [postId]

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
KEY_PATH = os.path.join(BASE_DIR, "..", "service-account.json")

def init():
    if not os.path.exists(KEY_PATH):
        print(f"ERROR: Missing service account key at {KEY_PATH}")
        sys.exit(1)

    cred = credentials.Certificate(KEY_PATH)
    firebase_admin.initialize_app(cred)

def send_test(post_id):
    topic = 'all_posts'
    
    message = messaging.Message(
        notification=messaging.Notification(
            title='Test Post Notification',
            body='Click to open this specific post!'
        ),
        data={
            'type': 'wall_post',
            'postId': post_id,
        },
        topic=topic,
    )

    response = messaging.send(message)
    print('Successfully sent message:', response)

if __name__ == "__main__":
    init()
    post_id = "test_post_id"
    if len(sys.argv) > 1:
        post_id = sys.argv[1]
    
    print(f"Sending test notification for Post ID: {post_id}")
    send_test(post_id)
