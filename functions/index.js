const functions = require('firebase-functions');
const admin = require('firebase-admin');
admin.initializeApp();

exports.sendFriendRequestNotification = functions.firestore
    .document('users/{userId}/friendRequests/{requestId}')
    .onCreate(async (snapshot, context) => {
        const requestData = snapshot.data();
        const receiverId = requestData.receiverId;
        const senderName = requestData.senderName;

        const payload = {
            notification: {
                title: "New Friend Request",
                body: `${senderName} sent you a friend request!`
            }
        };

        const userDoc = await admin.firestore().collection('users').doc(receiverId).get();
        const userToken = userDoc.data().fcmToken;

        if (userToken) {
            await admin.messaging().sendToDevice(userToken, payload);
        }
    });
