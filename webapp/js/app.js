if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('./sw.js').catch((err) => {
      console.log('SW registration failed:', err);
    });
  });
}

let deferredPrompt;
const installButton = document.createElement('button');
installButton.id = 'install-btn';
installButton.textContent = 'Als App installieren';
installButton.style.cssText = 'position:fixed;bottom:20px;left:50%;transform:translateX(-50%);background:#34aa8d;color:#fff;padding:12px 24px;border:none;border-radius:12px;z-index:9999;display:none;box-shadow:0 8px 24px rgba(52,170,141,0.4);font-weight:600;cursor:pointer;';

window.addEventListener('beforeinstallprompt', (e) => {
  e.preventDefault();
  deferredPrompt = e;
  installButton.style.display = 'block';
  document.body.appendChild(installButton);
});

installButton.addEventListener('click', async () => {
  if (!deferredPrompt) return;
  installButton.style.display = 'none';
  deferredPrompt.prompt();
  await deferredPrompt.userChoice;
  deferredPrompt = null;
});

window.addEventListener('appinstalled', () => {
  installButton.style.display = 'none';
  deferredPrompt = null;
});
