import csv
import firebase_admin
from firebase_admin import credentials, firestore

# Initialize Firebase
def initialize_firebase():
    try:
        cred = credentials.Certificate("service-account-key.json")
        firebase_admin.initialize_app(cred)
        print("Firebase initialized successfully!")
    except Exception as e:
        print(f"Error initializing Firebase: {e}")
        exit()

# Check for duplicate book titles
def is_duplicate_book(title, db, collection_name):
    try:
        docs = db.collection(collection_name).where("title", "==", title).get()
        return len(docs) > 0
    except Exception as e:
        print(f"Error checking for duplicate: {e}")
        return False

# Convert gs:// URI to https:// public URL
def convert_gs_to_https(gs_uri):
    try:
        bucket_name = gs_uri.split("gs://")[1].split("/")[0]
        file_path = "/".join(gs_uri.split("/")[1:])
        return f"https://firebasestorage.googleapis.com/v0/b/{bucket_name}/o/{file_path.replace('/', '%2F')}?alt=media"
    except Exception as e:
        print(f"Error converting gs:// URI to https:// URL: {e}")
        return None

# Upload CSV to Firestore
def upload_csv_to_firestore(csv_file_path, collection_name):
    try:
        db = firestore.client()
        with open(csv_file_path, mode="r", encoding="utf-8-sig") as file:
            reader = csv.DictReader(file)

            # Ensure column names are properly trimmed
            reader.fieldnames = [name.strip() for name in reader.fieldnames] if reader.fieldnames else []

            for row in reader:
                row = {key.strip(): value.strip() for key, value in row.items()}  # Trim spaces
                title = row.get("title")

                if not title:
                    print(f"Skipping row due to missing title: {row}")
                    continue

                if is_duplicate_book(title, db, collection_name):
                    print(f"Duplicate found. Skipping: {title}")
                    continue

                required_fields = ["title", "author", "category", "coverUrl", "pdfUrl"]
                missing_fields = [field for field in required_fields if not row.get(field)]
                if missing_fields:
                    print(f"Skipping {title} due to missing fields: {', '.join(missing_fields)}")
                    continue

                # Convert gs:// URIs if needed
                if row["coverUrl"].startswith("gs://"):
                    row["coverUrl"] = convert_gs_to_https(row["coverUrl"])
                if row["pdfUrl"].startswith("gs://"):
                    row["pdfUrl"] = convert_gs_to_https(row["pdfUrl"])

                row["timestamp"] = firestore.SERVER_TIMESTAMP
                db.collection(collection_name).add(row)
                print(f"Uploaded: {row}")

        print(f"All valid records from {csv_file_path} uploaded successfully to the '{collection_name}' collection!")
    except Exception as e:
        print(f"Error uploading data: {e}")

if __name__ == "__main__":
    csv_file_path = "books.csv"
    collection_name = "books"

    initialize_firebase()
    upload_csv_to_firestore(csv_file_path, collection_name)
