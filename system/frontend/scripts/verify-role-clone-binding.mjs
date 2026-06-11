import { readFileSync } from 'node:fs'
import { fileURLToPath } from 'node:url'
import { dirname, resolve } from 'node:path'

const root = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const usersView = readFileSync(resolve(root, 'src/views/user/UsersView.vue'), 'utf8')

const checks = [
  [
    'clone mode tracks the selected template role',
    usersView.includes('roleFormMode') && usersView.includes('templateRoleId')
  ],
  [
    'clone modal renders a real existing-role selector',
    /<select[\s\S]*v-model="newRoleForm\.templateRoleId"[\s\S]*v-for="role in cloneableRoles"/.test(usersView)
  ],
  [
    'top clone button opens the clone flow instead of showing a toast',
    /function cloneRoleTemplate\(\)\s*\{\s*openCloneRole\(\)\s*\}/.test(usersView)
  ],
  [
    'card clone button opens the clone flow with that role as template',
    /function cloneSingleRole\(role\)\s*\{\s*openCloneRole\(role\)\s*\}/.test(usersView)
  ],
  [
    'role creation submits permission codes from the selected template',
    /permissionCodes:\s*getRolePermissionCodesForSubmit\(\)/.test(usersView)
  ],
  [
    'changing the template refreshes the generated clone name',
    usersView.includes('@change="syncCloneTemplateFields"')
  ]
]

const failures = checks.filter(([, pass]) => !pass)

if (failures.length) {
  console.error('Role clone binding verification failed:')
  for (const [name] of failures) {
    console.error(`- ${name}`)
  }
  process.exit(1)
}

console.log('Role clone binding verification passed')
