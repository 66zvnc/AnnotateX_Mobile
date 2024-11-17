const functions = require("firebase-functions");
const admin = require("firebase-admin");
admin.initializeApp();

exports.sendFriendRequestNotification = functions.firestore
  .document("users/{userId}/friendRequests/{requestId}")
  .onCreate(async (snap, context) => {
    const requestData = snap.data();
    const receiverId = context.params.userId;
    const senderName = requestData.senderName;

    if (!requestData || !receiverId) {
      console.log("No request data or receiver ID");
      return;
    }

    try {
      // Get the receiver's FCM token from Firestore
      const userDoc = await admin.firestore().collection("users").doc(receiverId).get();
      const fcmToken = userDoc.data()?.fcmToken;

      if (!fcmToken) {
        console.log("No FCM token found for user");
        return;
      }

      const message = {
        notification: {
          title: "New Friend Request",
          body: `${senderName} sent you a friend request!`,
        },
        token: fcmToken,
      };

      // Send the notification
      const response = await admin.messaging().send(message);
      console.log("Notification sent successfully:", response);
    } catch (error) {
      console.error("Error sending notification:", error);
    }
  });
