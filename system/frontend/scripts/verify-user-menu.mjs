import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const layout = readFileSync(resolve(root, 'src/layout/AdminLayout.vue'), 'utf8')
const styles = readFileSync(resolve(root, 'src/assets/styles.css'), 'utf8')

const checks = [
  ['AdminLayout wraps the avatar trigger and menu together', layout.includes('class="user-menu-wrapper"')],
  ['avatar trigger toggles the menu instead of logging out directly', layout.includes('@click.stop="toggleUserMenu"')],
  ['avatar trigger exposes menu expanded state', layout.includes(':aria-expanded="String(userMenuOpen)"')],
  ['AdminLayout renders a conditional user menu', layout.includes('class="user-menu"') && layout.includes('v-if="userMenuOpen"')],
  ['user menu contains a dedicated logout button', layout.includes('class="logout-action"') && layout.includes('@click="handleLogout"')],
  ['avatar trigger no longer calls handleLogout directly', !/class="user"[\s\S]{0,160}@click="handleLogout"/.test(layout)],
  ['script tracks userMenuOpen state', layout.includes("const userMenuOpen = ref(false)")],
  ['script defines a toggleUserMenu handler', layout.includes('function toggleUserMenu()')],
  ['script closes the menu on outside document click', layout.includes("document.addEventListener('click', closeUserMenu)")],
  ['script removes the outside click listener on unmount', layout.includes("document.removeEventListener('click', closeUserMenu)")],
  ['logout flow closes the menu before route replacement', /async function handleLogout\(\)[\s\S]*closeUserMenu\(\)[\s\S]*router\.replace\('\/login'\)/.test(layout)],
  ['styles position the user menu wrapper', styles.includes('.user-menu-wrapper {') && styles.includes('position: relative')],
  ['styles define the dropdown panel', styles.includes('.user-menu {') && styles.includes('position: absolute')],
  ['styles right-align the dropdown under the avatar', styles.includes('right: 0') && styles.includes('top: calc(100% + 8px)')],
  ['styles define the logout action button', styles.includes('.logout-action {')],
  ['styles keep the mobile avatar menu usable', styles.includes('@media (max-width: 720px)') && styles.includes('.user-menu {')]
]

const failures = checks.filter(([, pass]) => !pass)

if (failures.length) {
  console.error('User menu verification failed:')
  for (const [name] of failures) {
    console.error(`- ${name}`)
  }
  process.exit(1)
}

console.log('User menu verification passed')
