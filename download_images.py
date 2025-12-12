import os
import requests
import time

base_url = "https://seedfundapi.startupindia.gov.in:3535/Portfolio/{}/Hero_Image.jpeg"
output_dir = "app/src/main/assets/incubator_logos"

os.makedirs(output_dir, exist_ok=True)

for i in range(1, 41):
    url = base_url.format(i)
    # Format i as 001, 002, etc.
    file_name = f"inc_{i:03d}.jpeg"
    file_path = os.path.join(output_dir, file_name)
    
    print(f"Downloading {url} to {file_path}...")
    
    try:
        response = requests.get(url, timeout=10)
        if response.status_code == 200:
            with open(file_path, 'wb') as f:
                f.write(response.content)
            print("Success")
        else:
            print(f"Failed with status {response.status_code}")
    except Exception as e:
        print(f"Error: {e}")
    
    time.sleep(0.5) # Be nice to server
