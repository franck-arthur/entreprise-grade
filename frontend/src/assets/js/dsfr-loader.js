/**
 * Custom DSFR loader that disables auto-initialization
 * and provides manual control.
 */

// Disable DSFR auto-initialization by setting this before DSFR loads
window.dsfrConfig = {
  autoInit: false,
  verbose: true
};

// Provide manual initialization function
window.initDsfrManually = function() {
  try {
    console.log('Attempting manual DSFR initialization...');

    if (typeof window.dsfr !== 'undefined') {
      // Check DOM readiness first
      const headerElements = document.querySelectorAll('.fr-header');
      if (headerElements.length === 0) {
        console.log('No header elements found, skipping DSFR initialization');
        return false;
      }

      // Try different initialization methods
      if (window.dsfr.start && typeof window.dsfr.start === 'function') {
        window.dsfr.start();
        console.log('DSFR started via dsfr.start()');
      } else if (window.dsfr.core && window.dsfr.core.start) {
        window.dsfr.core.start();
        console.log('DSFR started via dsfr.core.start()');
      } else if (window.dsfr.core && window.dsfr.core.init) {
        window.dsfr.core.init();
        console.log('DSFR started via dsfr.core.init()');
      } else {
        console.warn('No valid DSFR initialization method found');
        return false;
      }

      return true;
    } else {
      console.warn('DSFR not available on window object');
      return false;
    }
  } catch (error) {
    console.error('Manual DSFR initialization failed:', error);
    return false;
  }
};

// Provide a safer initialization function that checks DOM readiness
window.safeDsfrInit = function() {
  // Wait for elements to be ready
  const checkAndInit = () => {
    const headerElements = document.querySelectorAll('.fr-header');
    if (headerElements.length > 0) {
      return window.initDsfrManually();
    } else {
      console.log('Header elements not ready, deferring...');
      return false;
    }
  };

  // Try immediate initialization
  if (checkAndInit()) {
    return true;
  }

  // If failed, retry after a delay
  setTimeout(() => {
    if (!checkAndInit()) {
      console.log('Final DSFR initialization attempt after delay...');
      setTimeout(() => checkAndInit(), 500);
    }
  }, 200);

  return false;
};