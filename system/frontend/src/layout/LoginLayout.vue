<template>
  <div class="login-layout">
    <div class="stage"></div>
    <RouterView />
  </div>
</template>

<style scoped>
.login-layout {
  --bg-deeper: #061236;
  margin: 0;
  padding: 0;
  min-height: 100vh;
  min-height: 100dvh;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
  background-color: var(--bg-deeper);
  position: relative;
}

/* ───────── stage ───────── */
.stage {
  position: fixed;
  inset: 0;
  background-color: var(--bg-deeper);
  background-image: url("@/assets/back_login.png");
  background-size: cover;
  background-position: left center;
  background-repeat: no-repeat;
  z-index: 0;
}

/* gradient to keep the right-side card legible regardless of image edge */
.stage::after {
  content: "";
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse 60% 70% at 80% 50%, rgba(8, 32, 90, 0.55), transparent 70%),
    linear-gradient(90deg, transparent 0%, transparent 45%, rgba(4, 12, 42, 0.35) 70%, rgba(4, 12, 42, 0.55) 100%);
  pointer-events: none;
}

/* tablet: hero hidden, card centered, background re-anchored */
@media (max-width: 1024px) {
  .stage {
    background-position: center center;
    background-size: cover;
  }
  .stage::after {
    background:
      radial-gradient(ellipse 80% 60% at 50% 50%, rgba(8, 32, 90, 0.45), transparent 70%),
      linear-gradient(180deg, rgba(4, 12, 42, 0.30), rgba(4, 12, 42, 0.55));
  }
}

/* mobile */
@media (max-width: 640px) {
  /* simplify the background on mobile — image off, gradient on */
  .stage {
    background-image:
      radial-gradient(ellipse 90% 60% at 50% 0%, rgba(47, 124, 255, 0.28), transparent 65%),
      radial-gradient(ellipse 70% 50% at 80% 100%, rgba(30, 99, 224, 0.22), transparent 60%),
      linear-gradient(180deg, #061236 0%, #08184a 60%, #061236 100%);
  }
  .stage::after {
    background: none;
  }
}
</style>
