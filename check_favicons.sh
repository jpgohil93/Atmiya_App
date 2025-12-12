#!/bin/bash
domains=(
"fiire.org.in"
"iimklive.org"
"ciic.in"
"bhau.org"
"icreate.org.in"
"iiml.ac.in"
"siicincubator.com"
"aicbimtech.in"
"nsrcel.org"
"villgro.org"
"kiittbi.com"
"fitt-iitd.in"
"t-hub.co"
"cie.iiit.ac.in"
"startupoasis.in"
"sineiitb.org"
"amity.edu"
"iiitd.ac.in"
"sidbi.in"
"aicraise.com"
"iitm.ac.in"
"wfglobal.org"
"ahduni.edu.in"
"sid.iisc.ac.in"
"10000startups.com"
"mutbi.com"
"aicccmb.com"
"tides.iitr.ac.in"
"step.iitkgp.ac.in"
"nitt.edu"
"forgeinno.com"
"aicsangam.com"
"jntuh.ac.in"
"startupmission.kerala.gov.in"
"ciie.co"
"iimcal.ac.in"
"iitg.ac.in"
"isb.edu"
"bits-pilani.ac.in"
"venturecenter.co.in"
)

echo "Checking domains..."
for domain in "${domains[@]}"; do
    url="https://www.google.com/s2/favicons?domain=$domain&sz=256"
    status=$(curl -o /dev/null -s -w "%{http_code}" -L "$url")
    if [ "$status" != "200" ]; then
        echo "FAIL: $domain ($status)"
    else
        echo "OK: $domain"
    fi
done
