import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = [
        'name', 'description', 'colorInputs', 'customColorPicker', 'customColorDisplay', 'customColorIcon',
        'emojiInput', 'imageInput', 'imagePreview', 'imagePlaceholder', 'customSvgInput',
        'modeBtn', 'iconSection',
        'roleList', 'memberSelect', 'organMemberList',
        'roleModal', 'modalRoleName', 'modalRoleEmoji', 'permissionList',
        'submitBtn', 'spinner', 'error', 'loadingOverlay', 'loadingText'
    ];

    static values = {
        apiUrl: String,
        projectUuid: String,
        projectMembers: Array,
        availablePermissions: Array,
        organ: { type: Object, default: {} },
        userPermissions: { type: Array, default: [] }
    };

    connect() {
        this.iconMode = 'BLOB';
        
        this.presets = {
            responsible: {
                name: trans('organ.role_presets.responsible.name'),
                iconData: '👑',
                description: trans('organ.role_presets.responsible.description'),
                permissions: this.availablePermissionsValue.map(p => p.name)
            },
            manager: {
                name: trans('organ.role_presets.manager.name'),
                iconData: '📂',
                description: trans('organ.role_presets.manager.description'),
                permissions: this.availablePermissionsValue.map(p => p.name).filter(p => !['ORGAN_MANAGE_ROLES', 'ORGAN_MANAGE_MEMBERS'].includes(p))
            },
            participant: {
                name: trans('organ.role_presets.participant.name'),
                iconData: '👨‍💻',
                description: trans('organ.role_presets.participant.description'),
                permissions: [
                    'ORGAN_VIEW', 
                    'TASK_CREATE', 'TASK_EDIT_OWN', 'TASK_DELETE_OWN', 'TASK_STATUS_CHANGE_OWN', 'TASK_PRIORITY_CHANGE_OWN', 
                    'TASK_DATES_MANAGE_OWN', 'TASK_ESTIMATE_MANAGE_OWN', 'TASK_ASSIGN_SELF', 
                    'TASK_LINK_MANAGE_OWN', 'TASK_TAG_MANAGE_OWN', 'TASK_DEPENDENCY_MANAGE_OWN',
                    'COMMENT_CREATE', 'COMMENT_EDIT_OWN', 'COMMENT_DELETE_OWN', 
                    'ATTACHMENT_ADD', 'ATTACHMENT_DELETE_OWN'
                ]
            },
            developer: {
                name: trans('organ.role_presets.developer.name'),
                iconData: '⚙️',
                description: trans('organ.role_presets.developer.description'),
                permissions: [
                    'ORGAN_VIEW', 
                    'TASK_EDIT_OWN', 'TASK_STATUS_CHANGE_OWN', 'TASK_ESTIMATE_MANAGE_OWN', 'TASK_ASSIGN_SELF', 
                    'TASK_TAG_MANAGE_OWN', 'TASK_DEPENDENCY_MANAGE_OWN',
                    'COMMENT_CREATE', 'COMMENT_EDIT_OWN', 
                    'ATTACHMENT_ADD', 'ATTACHMENT_DELETE_OWN'
                ]
            },
            reviewer: {
                name: trans('organ.role_presets.reviewer.name'),
                iconData: '✅',
                description: trans('organ.role_presets.reviewer.description'),
                permissions: [
                    'ORGAN_VIEW', 
                    'TASK_EDIT_ALL', 'TASK_STATUS_CHANGE_ALL', 'TASK_VALIDATE', 'TASK_PRIORITY_CHANGE_ALL', 'TASK_DATES_MANAGE_ALL',
                    'COMMENT_CREATE', 'COMMENT_EDIT_OWN', 'COMMENT_DELETE_ALL',
                    'ATTACHMENT_ADD'
                ]
            },
            tester: {
                name: trans('organ.role_presets.tester.name'),
                iconData: '🔍',
                description: trans('organ.role_presets.tester.description'),
                permissions: [
                    'ORGAN_VIEW', 
                    'TASK_CREATE', 'TASK_STATUS_CHANGE_OWN', 
                    'COMMENT_CREATE', 
                    'ATTACHMENT_ADD'
                ]
            },
            observer: {
                name: trans('organ.role_presets.observer.name'),
                iconData: '👁️',
                description: trans('organ.role_presets.observer.description'),
                permissions: ['ORGAN_VIEW', 'COMMENT_CREATE']
            },
            guest: {
                name: trans('organ.role_presets.guest.name'),
                iconData: '✉️',
                description: trans('organ.role_presets.guest.description'),
                permissions: ['ORGAN_VIEW']
            }
        };

        if (this.hasOrganValue && this.organValue && this.organValue.uuid) {
            // Edit mode initialization
            this.iconMode = this.organValue.iconType || 'BLOB';
            
            // Ensure roles is an array even if empty or forced to object by some logic
            const rolesSource = Array.isArray(this.organValue.roles) 
                ? this.organValue.roles 
                : Object.values(this.organValue.roles || {});

            this.roles = rolesSource.map(r => ({
                id: r.uuid, // Use server UUID as local ID in edit mode
                name: r.name,
                iconType: r.iconType,
                iconData: r.iconData,
                permissions: Array.isArray(r.permissions) ? r.permissions : Object.values(r.permissions || [])
            }));

            // Build members list
            const membersMap = {}; // userUuid -> member object
            rolesSource.forEach(role => {
                const roleMembers = Array.isArray(role.members) ? role.members : Object.values(role.members || {});
                roleMembers.forEach(m => {
                    if (!membersMap[m.uuid]) {
                        const name = [m.firstName, m.lastName].filter(Boolean).join(' ') || m.email || 'Membre';
                        membersMap[m.uuid] = {
                            userUuid: m.uuid,
                            name: name,
                            email: m.email,
                            roles: []
                        };
                    }
                    membersMap[m.uuid].roles.push(role.uuid);
                });
            });
            this.addedMembers = Object.values(membersMap);
            
            // Set icon preview
            setTimeout(() => {
                if (this.iconMode === 'BLOB') {
                    this.imagePreviewTarget.src = this.organValue.iconData;
                    this.imagePreviewTarget.classList.remove('hidden');
                    this.imagePlaceholderTarget.classList.add('hidden');
                } else if (this.iconMode === 'EMOJI') {
                    this.emojiInputTarget.value = this.organValue.iconData;
                } else if (this.iconMode === 'SVG') {
                    this.customSvgInputTarget.value = this.organValue.iconData;
                }

                // Set color
                const color = this.organValue.highlightColor;
                const standardRadio = this.colorInputsTargets.find(input => input.value === color);
                if (standardRadio) {
                    standardRadio.checked = true;
                } else {
                    const customRadio = this.colorInputsTargets.find(input => input.value === 'custom');
                    if (customRadio) {
                        customRadio.checked = true;
                        this.customColorPickerTarget.value = color;
                        this.customColorDisplayTarget.style.backgroundColor = color;
                        this.customColorIconTarget.classList.add('hidden');
                    }
                }
                
                // Update mode UI
                this.iconSectionTargets.forEach(section => {
                    section.classList.toggle('hidden', section.dataset.mode !== this.iconMode);
                });
                this.modeBtnTargets.forEach(btn => {
                    const isActive = btn.dataset.mode === this.iconMode;
                    btn.classList.toggle('bg-white', isActive);
                    btn.classList.toggle('shadow-sm', isActive);
                    btn.classList.toggle('text-gray-900', isActive);
                    btn.classList.toggle('text-gray-500', !isActive);
                });
            }, 0);
        } else {
            // Create mode initialization
            this.roles = [
                { id: 'role-' + Date.now() + Math.random(), ...this.presets.responsible, iconType: 'EMOJI' }
            ];
            this.addedMembers = [];
        }

        this.editingRoleId = null;

        this.renderRoles();
        this.renderMembers();
    }

    hasPermission(permission) {
        if (!this.hasOrganValue || !this.organValue || !this.organValue.uuid) return true; // Creation mode: full access
        return this.userPermissionsValue.includes(permission) || this.userPermissionsValue.includes('ALL');
    }

    addPresetRole(event) {
        if (!this.hasPermission('ORGAN_MANAGE_ROLES')) return;
        const btn = event.currentTarget;
        const presetKey = btn.dataset.preset;
        const preset = this.presets[presetKey];
        if (!preset) return;

        const newRole = {
            id: 'role-' + Date.now() + Math.random(),
            name: preset.name,
            iconType: 'EMOJI',
            iconData: preset.iconData,
            permissions: [...preset.permissions]
        };
        
        this.roles.push(newRole);
        this.renderRoles();
        this.renderMembers();
        
        // Remove focus immediately
        btn.blur();

        // Punchy visual feedback
        btn.classList.add('scale-[1.02]', 'border-bubblegum/50', 'bg-white', 'shadow-md');
        setTimeout(() => {
            btn.classList.remove('scale-[1.02]', 'border-bubblegum/50', 'bg-white', 'shadow-md');
        }, 200);
    }

    // --- IDENTITY (Similar to Project) ---
    handleColorChange(event) {
        if (event.target.value === 'custom') {
            this.customColorPickerTarget.click();
        } else {
            this.customColorDisplayTarget.style.backgroundColor = '';
            this.customColorIconTarget.classList.remove('hidden');
        }
    }

    triggerColorPicker() {
        this.customColorPickerTarget.click();
    }

    handleCustomColorInput(event) {
        const color = event.target.value;
        this.customColorDisplayTarget.style.backgroundColor = color;
        this.customColorIconTarget.classList.add('hidden');
        const customRadio = this.colorInputsTargets.find(input => input.value === 'custom');
        if (customRadio) customRadio.checked = true;
    }

    switchIconMode(event) {
        const mode = event.currentTarget.dataset.mode;
        this.iconMode = mode;
        this.modeBtnTargets.forEach(btn => {
            const isActive = btn.dataset.mode === mode;
            btn.classList.toggle('bg-white', isActive);
            btn.classList.toggle('shadow-sm', isActive);
            btn.classList.toggle('text-gray-900', isActive);
            btn.classList.toggle('text-gray-500', !isActive);
        });
        this.iconSectionTargets.forEach(section => {
            section.classList.toggle('hidden', section.dataset.mode !== mode);
        });
    }

    handleImagePreview(event) {
        const file = event.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => {
                this.imagePreviewTarget.src = e.target.result;
                this.imagePreviewTarget.classList.remove('hidden');
                this.imagePlaceholderTarget.classList.add('hidden');
            };
            reader.readAsDataURL(file);
        }
    }

    validateEmojiInput(event) {
        const val = event.target.value.trim();
        if (!val) return;

        // Use Intl.Segmenter to properly handle multi-character emojis (like flags, skin tones)
        const segmenter = new Intl.Segmenter(undefined, { granularity: 'grapheme' });
        const segments = Array.from(segmenter.segment(val));
        const firstGrapheme = segments[0]?.segment;

        // Regex for emojis
        const emojiRegex = /\p{Extended_Pictographic}/u;

        if (firstGrapheme && emojiRegex.test(firstGrapheme)) {
            event.target.value = firstGrapheme;
            this.errorTarget.classList.add('hidden');
        } else {
            event.target.value = '';
            this.showError("Veuillez saisir un émoji valide.");
        }
    }

    // --- ROLE MANAGEMENT ---
    addRole() {
        if (!this.hasPermission('ORGAN_MANAGE_ROLES')) return;
        const newRole = {
            id: 'role-' + Date.now(),
            name: 'Nouveau rôle',
            iconType: 'EMOJI',
            iconData: '👤',
            permissions: []
        };
        this.roles.push(newRole);
        this.renderRoles();
    }

    removeRole(event) {
        const id = event.currentTarget.dataset.id;
        if (this.roles.length <= 1) return;
        this.roles = this.roles.filter(r => r.id !== id);
        // Also remove this role from members
        this.addedMembers.forEach(m => {
            m.roles = m.roles.filter(rid => rid !== id);
        });
        this.renderRoles();
        this.renderMembers();
    }

    openRoleConfig(event) {
        const id = event.currentTarget.dataset.id;
        const role = this.roles.find(r => r.id === id);
        if (!role) return;

        this.editingRoleId = id;
        this.modalRoleNameTarget.value = role.name;
        this.modalRoleEmojiTarget.value = (role.iconType === 'EMOJI') ? role.iconData : '';
        
        const organPerms = this.availablePermissionsValue.filter(p => 
            p.name.startsWith('ORGAN_') && p.name !== 'ORGAN_MANAGE_MEMBERS'
        );
        const taskPerms = this.availablePermissionsValue.filter(p => p.name.startsWith('TASK_'));
        const interactionPerms = this.availablePermissionsValue.filter(p => p.name.startsWith('COMMENT_') || p.name.startsWith('ATTACHMENT_'));

        const renderPermGroup = (title, perms) => `
            <div class="space-y-4">
                <h4 class="text-xs font-black uppercase tracking-widest text-gray-400 border-b border-gray-100 pb-2">${title}</h4>
                <div class="grid grid-cols-1 md:grid-cols-2 gap-3">
                    ${perms.map(perm => {
                        const label = trans(`organ.permissions.${perm.name}.label`);
                        const desc = trans(`organ.permissions.${perm.name}.description`);
                        const isMandatory = perm.name === 'ORGAN_VIEW';
                        
                        // Check if role has this permission OR if it has the coupled member permission (when rendering roles perm)
                        let isChecked = role.permissions.includes(perm.name) || isMandatory;
                        if (perm.name === 'ORGAN_MANAGE_ROLES' && role.permissions.includes('ORGAN_MANAGE_MEMBERS')) {
                            isChecked = true;
                        }

                        return `
                            <label class="relative flex items-start gap-4 p-4 bg-gray-50 rounded-2xl cursor-pointer hover:bg-gray-100 transition-all border border-transparent has-[:checked]:border-bubblegum/30 has-[:checked]:bg-bubblegum/[0.02] group ${isMandatory ? 'opacity-70 pointer-events-none' : ''}">
                                <div class="mt-1 shrink-0">
                                    <input type="checkbox" class="peer sr-only" 
                                           value="${perm.name}" ${isChecked ? 'checked' : ''} ${isMandatory ? 'readonly disabled' : ''}>
                                    
                                    <!-- Modern Custom Checkbox -->
                                    <div class="w-5 h-5 border-2 border-gray-300 rounded-md bg-white transition-all duration-200 
                                                peer-checked:bg-bubblegum peer-checked:border-bubblegum 
                                                peer-focus:ring-2 peer-focus:ring-bubblegum/20
                                                group-hover:border-bubblegum/50 flex items-center justify-center shrink-0">
                                        <svg class="w-3.5 h-3.5 text-white opacity-0 peer-checked:opacity-100 transition-opacity duration-200" fill="none" viewBox="0 0 24 24" stroke="currentColor" stroke-width="4">
                                            <path stroke-linecap="round" stroke-linejoin="round" d="M5 13l4 4L19 7" />
                                        </svg>
                                    </div>
                                </div>
                                <div class="flex-grow">
                                    <p class="text-[13px] font-bold text-gray-900 group-hover:text-bubblegum transition-colors">${label || perm.name}</p>
                                    <p class="text-[11px] text-gray-500 font-medium leading-tight mt-0.5">${desc || ''}</p>
                                </div>
                            </label>
                        `;
                    }).join('')}
                </div>
            </div>
        `;

        this.permissionListTarget.innerHTML = `
            <div class="space-y-10">
                ${renderPermGroup(trans('organ.permissions.categories.organ') || 'Gestion de l\'Organ', organPerms)}
                ${renderPermGroup(trans('organ.permissions.categories.tasks') || 'Gestion des Tâches', taskPerms)}
                ${renderPermGroup(trans('organ.permissions.categories.interactions') || 'Interactions & Fichiers', interactionPerms)}
            </div>
        `;

        this.roleModalTarget.classList.remove('hidden');
    }

    closeRoleModal() {
        this.roleModalTarget.classList.add('hidden');
        this.editingRoleId = null;
    }

    saveRole() {
        const role = this.roles.find(r => r.id === this.editingRoleId);
        if (!role) return;

        role.name = this.modalRoleNameTarget.value.trim() || 'Rôle sans nom';
        role.iconData = this.modalRoleEmojiTarget.value.trim() || '👤';
        role.iconType = 'EMOJI';

        const checkedPerms = Array.from(this.permissionListTarget.querySelectorAll('input:checked')).map(i => i.value);
        if (!checkedPerms.includes('ORGAN_VIEW')) checkedPerms.push('ORGAN_VIEW');
        
        // Couple Roles and Members management
        if (checkedPerms.includes('ORGAN_MANAGE_ROLES')) {
            if (!checkedPerms.includes('ORGAN_MANAGE_MEMBERS')) {
                checkedPerms.push('ORGAN_MANAGE_MEMBERS');
            }
        } else {
            // If Roles management is removed, also remove Members management to keep them coupled
            const index = checkedPerms.indexOf('ORGAN_MANAGE_MEMBERS');
            if (index !== -1) {
                checkedPerms.splice(index, 1);
            }
        }

        role.permissions = checkedPerms;

        this.renderRoles();
        this.renderMembers();
        this.closeRoleModal();
    }

    renderRoles() {
        const projectColor = this.element.style.getPropertyValue('--project-color') || '#FF7EB6';
        const projectColorLight = this.element.style.getPropertyValue('--project-color-light') || '#FF7EB61a';

        this.roleListTarget.innerHTML = this.roles.map(role => `
            <div class="bg-gray-50 p-4 rounded-2xl border border-gray-100 flex items-center justify-between group">
                <div class="flex items-center gap-3">
                    <div class="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center text-xl">
                        ${role.iconData || '👤'}
                    </div>
                    <div>
                        <p class="font-bold text-gray-900">${role.name}</p>
                        <p class="text-[10px] text-gray-400 uppercase font-black">${role.permissions.length} permissions</p>
                    </div>
                </div>
                <div class="flex items-center gap-2">
                    <button type="button" data-action="click->organ-create#openRoleConfig" data-id="${role.id}"
                            class="p-2 text-gray-400 transition-colors cursor-pointer hover:opacity-80"
                            style="color: ${projectColor}">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z"/><circle cx="12" cy="12" r="3"/></svg>
                    </button>
                    <button type="button" data-action="click->organ-create#removeRole" data-id="${role.id}"
                            class="p-2 text-gray-400 hover:text-red-500 transition-colors cursor-pointer ${this.roles.length <= 1 ? 'hidden' : ''}">
                        <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M3 6h18"/><path d="M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6"/><path d="M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2"/></svg>
                    </button>
                </div>
            </div>
        `).join('');
    }

    // --- MEMBER MANAGEMENT ---
    addMember() {
        const userUuid = this.memberSelectTarget.value;
        if (!userUuid) return;
        if (this.addedMembers.find(m => m.userUuid === userUuid)) return;

        const memberInfo = this.projectMembersValue.find(pm => pm.user.uuid === userUuid);
        if (!memberInfo) return;

        this.addedMembers.push({
            userUuid: userUuid,
            name: memberInfo.user.firstName,
            email: memberInfo.user.email,
            roles: []
        });

        this.memberSelectTarget.value = '';
        this.renderMembers();
    }

    removeMember(event) {
        const uuid = event.currentTarget.dataset.uuid;
        this.addedMembers = this.addedMembers.filter(m => m.userUuid !== uuid);
        this.renderMembers();
    }

    toggleMemberRole(event) {
        const userUuid = event.currentTarget.dataset.userUuid;
        const roleId = event.currentTarget.dataset.roleId;
        const member = this.addedMembers.find(m => m.userUuid === userUuid);
        if (!member) return;

        if (member.roles.includes(roleId)) {
            member.roles = member.roles.filter(id => id !== roleId);
        } else {
            member.roles.push(roleId);
        }
        this.renderMembers();
    }

    renderMembers() {
        const projectColor = this.element.style.getPropertyValue('--project-color') || '#FF7EB6';
        const canManageMembers = this.hasPermission('ORGAN_MANAGE_MEMBERS');

        if (this.addedMembers.length === 0) {
            this.organMemberListTarget.innerHTML = `<p class="text-sm text-gray-400 italic">Aucun membre ajouté à l'organ.</p>`;
            return;
        }

        this.organMemberListTarget.innerHTML = this.addedMembers.map(member => `
            <div class="bg-gray-50 p-4 rounded-2xl border border-gray-100 flex flex-col gap-4">
                <div class="flex items-center justify-between">
                    <div class="flex items-center gap-3">
                        <div class="w-10 h-10 bg-white rounded-xl shadow-sm flex items-center justify-center font-black"
                             style="color: ${projectColor}">
                            ${(member.name || '').charAt(0)}
                        </div>
                        <div>
                            <p class="text-sm font-bold text-gray-900">${member.name}</p>
                            <p class="text-[10px] text-gray-400 font-medium">${member.email}</p>
                        </div>
                    </div>
                    ${canManageMembers ? `
                        <button type="button" data-action="click->organ-create#removeMember" data-uuid="${member.userUuid}"
                                class="p-2 text-gray-300 hover:text-red-500 transition-colors cursor-pointer">
                            <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                        </button>
                    ` : ''}
                </div>
                
                <div class="flex flex-wrap gap-2">
                    ${this.roles.map(role => {
                        const isActive = member.roles.includes(role.id);
                        const style = isActive 
                            ? `background-color: ${projectColor}; border-color: ${projectColor}; color: white;`
                            : `background-color: white; border-color: #f3f4f6; color: #6b7280;`;
                        
                        const actionAttr = canManageMembers ? `data-action="click->organ-create#toggleMemberRole"` : '';
                        const cursorClass = canManageMembers ? 'cursor-pointer' : 'cursor-default';

                        return `
                            <button type="button" ${actionAttr} 
                                    data-user-uuid="${member.userUuid}" data-role-id="${role.id}"
                                    class="px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest transition-all border ${cursorClass}"
                                    style="${style}"
                                    ${canManageMembers ? `
                                        onmouseover="if(!${isActive}) this.style.borderColor='${projectColor}'; if(!${isActive}) this.style.color='${projectColor}';"
                                        onmouseout="if(!${isActive}) this.style.borderColor='#f3f4f6'; if(!${isActive}) this.style.color='#6b7280';"
                                    ` : ''}>
                                ${role.name}
                            </button>
                        `;
                    }).join('')}
                </div>
            </div>
        `).join('');
    }

    // --- SUBMISSION ---
    async submit() {
        this.errorTarget.classList.add('hidden');
        const name = this.nameTarget.value.trim();
        if (!name) {
            this.showError("Le nom de l'organ est obligatoire.");
            return;
        }

        this.submitBtnTarget.disabled = true;
        this.spinnerTarget.classList.remove('hidden');
        this.loadingOverlayTarget.classList.remove('hidden');

        const selectedColorInput = this.colorInputsTargets.find(input => input.checked);
        const color = (selectedColorInput && selectedColorInput.value !== 'custom') 
            ? selectedColorInput.value 
            : this.customColorPickerTarget.value;
        
        let iconType = this.iconMode;
        let iconData = '';
        if (this.iconMode === 'SVG') iconData = this.customSvgInputTarget.value.trim();
        else if (this.iconMode === 'EMOJI') iconData = this.emojiInputTarget.value || '🚀';
        else if (this.iconMode === 'BLOB') iconData = this.imagePreviewTarget.src;

        const isEdit = !!(this.hasOrganValue && this.organValue && this.organValue.uuid);
        const organUuid = isEdit ? this.organValue.uuid : null;

        try {
            // STEP 1: CREATE OR UPDATE ORGAN
            let actualOrganUuid = organUuid;

            if (!isEdit || this.hasPermission('ORGAN_EDIT')) {
                this.loadingTextTarget.innerText = isEdit ? "Mise à jour de l'organ..." : "Création de l'organ...";
                const organUrl = isEdit 
                    ? `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${organUuid}`
                    : `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs`;
                
                const organResponse = await fetch(organUrl, {
                    method: isEdit ? 'PATCH' : 'POST',
                    headers: { 'Content-Type': 'application/json' },
                    body: JSON.stringify({
                        title: name,
                        description: this.descriptionTarget.value.trim(),
                        highlightColor: color,
                        iconType: iconType,
                        iconData: iconData
                    }),
                    credentials: 'include'
                });

                if (!organResponse.ok) throw new Error(isEdit ? "Erreur lors de la mise à jour de l'organ." : "Erreur lors de la création de l'organ.");
                
                if (!isEdit) {
                    const createdOrgan = await organResponse.json();
                    actualOrganUuid = createdOrgan.uuid;
                }
            }

            const finalOrganUuid = actualOrganUuid;

            // STEP 2: ROLES MANAGEMENT
            if (!isEdit || this.hasPermission('ORGAN_MANAGE_ROLES')) {
                this.loadingTextTarget.innerText = "Synchronisation des rôles...";
                const roleIdMap = {}; // Local ID -> Server UUID

                if (isEdit) {
                    const existingRoles = this.organValue.roles || [];
                    
                    // Delete removed roles
                    for (const existingRole of existingRoles) {
                        if (!this.roles.find(r => r.id === existingRole.uuid)) {
                            await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${finalOrganUuid}/roles/${existingRole.uuid}`, {
                                method: 'DELETE',
                                credentials: 'include'
                            });
                        }
                    }

                    // Add or Update roles
                    for (const role of this.roles) {
                        const isNewRole = String(role.id).startsWith('role-');
                        const roleUrl = isNewRole 
                            ? `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${finalOrganUuid}/roles`
                            : `${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${finalOrganUuid}/roles/${role.id}`;
                        
                        const roleResponse = await fetch(roleUrl, {
                            method: isNewRole ? 'POST' : 'PATCH',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                name: role.name,
                                iconType: role.iconType,
                                iconData: role.iconData,
                                permissions: role.permissions
                            }),
                            credentials: 'include'
                        });
                        
                        if (!roleResponse.ok) throw new Error(`Erreur lors de la sync du rôle ${role.name}.`);
                        const roleData = await roleResponse.json();
                        roleIdMap[role.id] = roleData.uuid;
                    }
                } else {
                    // Creation mode
                    for (const role of this.roles) {
                        const roleResponse = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${finalOrganUuid}/roles`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({
                                name: role.name,
                                iconType: role.iconType,
                                iconData: role.iconData,
                                permissions: role.permissions
                            }),
                            credentials: 'include'
                        });
                        if (!roleResponse.ok) throw new Error(`Erreur lors de la création du rôle ${role.name}.`);
                        const roleData = await roleResponse.json();
                        roleIdMap[role.id] = roleData.uuid;
                    }
                }

                // STEP 3: ASSIGN MEMBERS (When roles managed)
                if (!isEdit || this.hasPermission('ORGAN_MANAGE_MEMBERS')) {
                    this.loadingTextTarget.innerText = "Attribution des membres...";
                    for (const member of this.addedMembers) {
                        for (const localRoleId of member.roles) {
                            const serverRoleUuid = roleIdMap[localRoleId] || localRoleId;
                            await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${finalOrganUuid}/roles/${serverRoleUuid}/assign`, {
                                method: 'POST',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ userUuid: member.userUuid }),
                                credentials: 'include'
                            }).catch(e => console.error("Assignment failed", e));
                        }
                    }
                }
            } else if (isEdit && this.hasPermission('ORGAN_MANAGE_MEMBERS')) {
                // If user ONLY has member permission in edit mode
                this.loadingTextTarget.innerText = "Mise à jour des membres...";
                for (const member of this.addedMembers) {
                    for (const roleId of member.roles) {
                        await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/organs/${finalOrganUuid}/roles/${roleId}/assign`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ userUuid: member.userUuid }),
                            credentials: 'include'
                        }).catch(e => console.error("Assignment failed", e));
                    }
                }
            }

            window.location.href = isEdit 
                ? `/projects/${this.projectUuidValue}/organs/${finalOrganUuid}`
                : `/projects/${this.projectUuidValue}`;

        } catch (e) {
            this.showError(e.message);
        }
    }

    showError(message) {
        this.errorTarget.innerText = message;
        this.errorTarget.classList.remove('hidden');
        this.submitBtnTarget.disabled = false;
        this.spinnerTarget.classList.add('hidden');
        this.loadingOverlayTarget.classList.add('hidden');
    }
}
