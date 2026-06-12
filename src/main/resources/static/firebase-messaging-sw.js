// firebase-messaging-sw.js — Service Worker pour les notifications push Firebase
// Ce fichier est servi sur /firebase-messaging-sw.js par Spring Boot (static/)
// La config Firebase est transmise depuis la page via postMessage

importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-messaging-compat.js');

let messagingInitialized = false;

// Réception de la config depuis la page (postMessage après enregistrement du SW)
self.addEventListener('message', function(event) {
  if (event.data && event.data.type === 'FIREBASE_CONFIG' && !messagingInitialized) {
    try {
      if (!firebase.apps.length) {
        firebase.initializeApp(event.data.config);
      }
      const messaging = firebase.messaging();
      messagingInitialized = true;

      // ─── Notifications en arrière-plan ─────────────────────────────────────
      messaging.onBackgroundMessage(function(payload) {
        const title = (payload.notification && payload.notification.title) || 'BOLA';
        const body  = (payload.notification && payload.notification.body)  || '';
        const link  = (payload.fcmOptions   && payload.fcmOptions.link)    || '/';
        self.registration.showNotification(title, {
          body:    body,
          icon:    '/images/bola-icon-192.png',
          badge:   '/images/bola-icon-192.png',
          vibrate: [200, 100, 200],
          data:    { url: link }
        });
      });
    } catch (e) {
      // Ignore — Firebase peut déjà être initialisé
    }
  }
});

// ─── Clic sur notification → ouvrir l'URL ────────────────────────────────────
self.addEventListener('notificationclick', function(event) {
  event.notification.close();
  const url = (event.notification.data && event.notification.data.url) || '/';
  event.waitUntil(
    clients.matchAll({ type: 'window', includeUncontrolled: true }).then(function(clientList) {
      for (var i = 0; i < clientList.length; i++) {
        var client = clientList[i];
        if (client.url.indexOf(url) !== -1 && 'focus' in client) return client.focus();
      }
      if (clients.openWindow) return clients.openWindow(url);
    })
  );
});
