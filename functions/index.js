const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

function generateOTP() {
  return Math.floor(100000 + Math.random() * 900000).toString();
}

exports.sendOTP = functions.https.onCall(async (data, context) => {
  const email = data.email;
  
  if (!email) {
    throw new functions.https.HttpsError('invalid-argument', 'Email is required');
  }

  const otp = generateOTP();
  const expiresAt = admin.firestore.Timestamp.fromDate(new Date(Date.now() + 5 * 60 * 1000));

  await admin.firestore().collection('otps').doc(email).set({
    otp: otp,
    expiresAt: expiresAt,
    createdAt: admin.firestore.FieldValue.serverTimestamp()
  });

  return { 
    success: true, 
    message: 'OTP generated',
    otp: otp // Remove this in production - for testing only
  };
});

exports.verifyOTP = functions.https.onCall(async (data, context) => {
  const { email, otp } = data;

  if (!email || !otp) {
    throw new functions.https.HttpsError('invalid-argument', 'Email and OTP are required');
  }

  const otpDoc = await admin.firestore().collection('otps').doc(email).get();

  if (!otpDoc.exists) {
    throw new functions.https.HttpsError('not-found', 'OTP not found or expired');
  }

  const otpData = otpDoc.data();
  const expiresAt = otpData.expiresAt.toDate();
  
  if (new Date() > expiresAt) {
    throw new functions.https.HttpsError('deadline-exceeded', 'OTP has expired');
  }

  if (otpData.otp !== otp) {
    throw new functions.https.HttpsError('unauthenticated', 'Invalid OTP');
  }

  await admin.firestore().collection('otps').doc(email).delete();

  return { success: true, message: 'OTP verified successfully' };
});