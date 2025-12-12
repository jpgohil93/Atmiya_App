#!/bin/bash
mkdir -p app/src/main/assets/incubator_logos
base_url="https://seedfundapi.startupindia.gov.in:3535/Portfolio"

for i in {1..40}; do
    # Format i with leading zeros
    printf -v i_padded "%03d" $i
    url="$base_url/$i/Hero_Image.jpeg"
    output="app/src/main/assets/incubator_logos/inc_$i_padded.jpeg"
    
    echo "Downloading $url to $output..."
    curl -s -o "$output" "$url"
done
