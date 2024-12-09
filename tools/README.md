Firebase CSV Upload Script - Quick Guide

How It Works
1.Initializes Firebase:

Authenticates using the service-account-key.json file.
Connects to Firestore.
Reads the CSV File:

Parses the provided books.csv file.
2.Validates Data:

Ensures required fields (title, author, category, coverUrl, pdfUrl) are present.
Checks for duplicate entries in Firestore by comparing the title.
Handles gs:// URIs:

Converts gs:// URIs to public https:// URLs.
3.Uploads Data:

Adds valid records to the Firestore books collection.
Skips invalid or duplicate rows.

--Prerequisites--

Install Python 3.7+.
Install Firebase Admin SDK:

$ pip install firebase-admin

Download the Firebase service account key:

Go to Firebase Console > Project Settings > Service Accounts > Generate New Private Key.
Save it as service-account-key.json.
File Requirements
Script: Save as upload_csv_to_firestore.py.
CSV File: Name it books.csv with headers:
csv
Copy code
title,author,category,coverUrl,pdfUrl,description
How to Run
Place upload_csv_to_firestore.py, books.csv, and service-account-key.json in the same folder.
Execute the script:

$ python upload_csv_to_firestore.py

Firestore Data Structure
Each book document will be added to the books collection with this structure:

--------------------------------------------------------------------------------

"title": "The Great Gatsby",
"author": "F. Scott Fitzgerald",
"category": "Classic",
"coverUrl": "https://firebasestorage.googleapis.com/v0/b/<bucket>/o/uploads%2Fgatsby.jpg?alt=media",
"pdfUrl": "https://firebasestorage.googleapis.com/v0/b/<bucket>/o/uploads%2Fgatsby.pdf?alt=media",
"description": "A novel set in the Roaring Twenties.",
"timestamp": "<timestamp>"

--------------------------------------------------------------------------------


Important Notes!
Duplicate Handling: The script skips duplicate entries based on the title field.
Missing Fields: Rows missing required fields are skipped.
Logs: Check the console for skipped rows, errors, and upload status.
