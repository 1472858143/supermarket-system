import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const usersView = readFileSync(resolve(root, 'src/views/user/UsersView.vue'), 'utf8')

const checks = [
  [
    'drawer panels open only from their own visible state class',
    !usersView.includes('.drawer-mask.show ~ .drawer-panel')
  ],
  [
    'user drawer panel is controlled by userDrawerVisible',
    /<aside\s+class="drawer-panel"\s+:class="\{\s*show:\s*userDrawerVisible\s*\}"/.test(usersView)
  ],
  [
    'permission drawer panel is controlled by permDrawerVisible',
    /<aside\s+id="permDrawer"\s+class="drawer-panel perm-drawer"\s+:class="\{\s*show:\s*permDrawerVisible\s*\}"/.test(usersView)
  ]
]

const failures = checks.filter(([, pass]) => !pass)

if (failures.length) {
  console.error('User permission drawer verification failed:')
  for (const [name] of failures) {
    console.error(`- ${name}`)
  }
  process.exit(1)
}

console.log('User permission drawer verification passed')
