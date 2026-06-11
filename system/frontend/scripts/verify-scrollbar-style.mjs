import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const styles = readFileSync(resolve(root, 'src/assets/styles.css'), 'utf8')

const checks = [
  ['global Firefox scrollbar is thin', /\*\s*\{[^}]*scrollbar-width:\s*thin;/m.test(styles)],
  ['global Firefox scrollbar uses theme colors', /scrollbar-color:\s*var\(--scrollbar-thumb\)\s+transparent;/.test(styles)],
  ['global WebKit scrollbar has a 6px width', /\*::\-webkit-scrollbar\s*\{[^}]*width:\s*6px;/m.test(styles)],
  ['global WebKit scrollbar has a 6px height', /\*::\-webkit-scrollbar\s*\{[^}]*height:\s*6px;/m.test(styles)],
  ['global WebKit track stays transparent', /\*::\-webkit-scrollbar-track(?:\s*,\s*\*::\-webkit-scrollbar-corner)?\s*\{[^}]*background:\s*transparent;/m.test(styles)],
  ['global WebKit thumb uses theme colors', /\*::\-webkit-scrollbar-thumb\s*\{[^}]*background:\s*var\(--scrollbar-thumb\);/m.test(styles)],
  ['global WebKit thumb has a hover state', /\*::\-webkit-scrollbar-thumb:hover\s*\{[^}]*background:\s*var\(--scrollbar-thumb-hover\);/m.test(styles)],
  ['sidebar scrollbar is also 6px wide', /\.nav::\-webkit-scrollbar\s*\{[^}]*width:\s*6px;/m.test(styles)]
]

const failures = checks.filter(([, pass]) => !pass)

if (failures.length) {
  console.error('Scrollbar style verification failed:')
  for (const [name] of failures) console.error(`- ${name}`)
  process.exit(1)
}

console.log('Scrollbar style verification passed')
