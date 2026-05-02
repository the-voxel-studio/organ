import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = [
        'name', 'description', 'status', 'colorInputs', 'customColorPicker', 'customColorDisplay', 'customColorIcon',
        'emojiInput', 'imageInput', 'imagePreview', 'imagePlaceholder', 'customSvgInput',
        'modeBtn', 'iconSection',
        'inviteEmail', 'inviteList', 'roleExplanation',
        'submitBtn', 'spinner', 'error',
        'deleteModal', 'deleteSubmitBtn', 'deleteSpinner',
        'googleModal',
        'driveStatus', 'googleBtnText', 'driveFolderSection', 'folderLoader', 'folderList', 'generateFolderBtn'
    ];

    static values = {
        userEmail: String,
        userName: String,
        apiUrl: String,
        projectUuid: String,
        initialMembers: Array,
        invitations: Array,
        googleClientId: String,
        userRole: String
    };

    connect() {
        this.iconMode = 'BLOB';
        this.removedMemberUuids = [];
        this.selectedFolderId = null;
        
        // Handle ESC key to close modal
        this.escHandler = (e) => {
            if (e.key === 'Escape' && this.hasDeleteModalTarget && !this.deleteModalTarget.classList.contains('hidden')) {
                this.closeDeleteModal();
            }
        };
        window.addEventListener('keydown', this.escHandler);
        
        if (this.hasProjectUuidValue && this.projectUuidValue) {
            // Edit mode members logic...
            this.invites = (this.initialMembersValue || []).map(m => {
                const isCreator = (m.user.email === this.userEmailValue);
                return {
                    uuid: m.uuid,
                    email: m.user.email,
                    name: m.user.firstName,
                    role: m.globalRole,
                    isCreator: isCreator,
                    showMenu: false,
                    isExisting: true,
                    isPending: false,
                    roleChanged: false
                };
            });

            const pendingInvites = (this.invitationsValue || []).map(inv => ({
                uuid: inv.uuid,
                email: inv.email,
                name: 'Invité',
                role: inv.role,
                isCreator: false,
                showMenu: false,
                isExisting: true,
                isPending: true,
                roleChanged: false
            }));
            
            this.invites = [...this.invites, ...pendingInvites];
            this.invites.sort((a, b) => b.isCreator - a.isCreator);

            if (this.hasImagePreviewTarget && !this.imagePreviewTarget.classList.contains('hidden')) this.iconMode = 'BLOB';
            else if (this.hasEmojiInputTarget && this.emojiInputTarget.value) this.iconMode = 'EMOJI';
            else if (this.hasCustomSvgInputTarget && this.customSvgInputTarget.value) this.iconMode = 'SVG';

            this.checkDriveConfig();
        } else {
            this.invites = [{ email: this.userEmailValue, name: this.userNameValue, role: 'ADMIN', isCreator: true, showMenu: false, isExisting: false }];
        }
        
        this.renderInvites();
    }

    // --- COLOR & ICON MANAGEMENT (Keeping unchanged) ---
    handleColorChange(event) { if (event.target.value === 'custom') this.customColorPickerTarget.click(); }
    triggerColorPicker() { this.customColorPickerTarget.click(); }
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
        this.modeBtnTargets.forEach(btn => btn.classList.toggle('bg-white', btn.dataset.mode === mode));
        this.iconSectionTargets.forEach(section => section.classList.toggle('hidden', section.dataset.mode !== mode));
    }
    validateEmojiInput(event) {
        const val = event.target.value.trim();
        if (!val) return;
        const emojiRegex = /\p{Extended_Pictographic}/u;
        if (emojiRegex.test(val)) { event.target.value = Array.from(val)[0]; }
        else { event.target.value = ''; }
    }
    handleImagePreview(event) {
        const file = event.target.files[0];
        if (file) {
            const reader = new FileReader();
            reader.onload = (e) => { this.imagePreviewTarget.src = e.target.result; this.imagePreviewTarget.classList.remove('hidden'); this.imagePlaceholderTarget.classList.add('hidden'); };
            reader.readAsDataURL(file);
        }
    }

    // --- GOOGLE DRIVE BYOS ---
    async checkDriveConfig() {
        if (!this.hasDriveFolderSectionTarget) return;
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/drive-config`, { credentials: 'include' });
            if (res.ok) {
                const config = await res.json();
                this.selectedFolderId = config.driveFolderId;
                if (config.isActive) {
                    this.driveStatusTarget.classList.remove('hidden');
                    this.googleBtnTextTarget.innerText = trans('project.create.drive.linked');
                    this.driveFolderSectionTarget.classList.remove('hidden');
                    if (this.selectedFolderId) {
                        this.generateFolderBtnTarget.classList.add('hidden');
                    }
                    await this.loadFolders();
                }
            }
        } catch (e) {}
    }

    openGoogleModal() { this.googleModalTarget.classList.remove('hidden'); }
    closeGoogleModal() { this.googleModalTarget.classList.add('hidden'); }

    async connectGoogle() {
        this.closeGoogleModal();
        if (!this.googleClientIdValue) return;

        if (typeof google === 'undefined') {
            await new Promise(resolve => {
                const script = document.createElement('script');
                script.src = 'https://accounts.google.com/gsi/client';
                script.onload = resolve;
                document.head.appendChild(script);
            });
        }

        const client = google.accounts.oauth2.initCodeClient({
            client_id: this.googleClientIdValue,
            scope: 'https://www.googleapis.com/auth/drive.file',
            ux_mode: 'popup',
            select_account: true,
            prompt: 'consent',
            callback: async (response) => {
                if (response.code) {
                    try {
                        const linkRes = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/drive-config/connect-google`, {
                            method: 'POST',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ authCode: response.code }),
                            credentials: 'include'
                        });

                        if (linkRes.ok) {
                            this.driveStatusTarget.classList.remove('hidden');
                            this.googleBtnTextTarget.innerText = trans('project.create.drive.linked');
                            this.driveFolderSectionTarget.classList.remove('hidden');
                            await this.loadFolders();
                        }
                    } catch (e) { console.error(e); }
                }
            },
        });
        client.requestCode();
    }

    async loadFolders() {
        if (!this.hasFolderLoaderTarget) return;
        this.folderLoaderTarget.classList.remove('hidden');
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/drive-config/list-folders`, { credentials: 'include' });
            if (res.ok) {
                const folders = await res.json();
                // Filter out the root folder if it's named "Organ App" (already filtered by API usually)
                this.renderFolders(folders.filter(f => f.name !== "Organ App"));
            }
        } catch (e) { console.error(e); }
        finally { this.folderLoaderTarget.classList.add('hidden'); }
    }

    renderFolders(folders) {
        this.folderListTarget.innerHTML = '';
        if (folders.length === 0) {
            this.folderListTarget.innerHTML = `<p class="text-xs text-gray-400 italic p-4 text-center">${trans('project.create.drive.empty_folders')}</p>`;
            return;
        }

        folders.forEach(f => {
            const isSelected = f.id === this.selectedFolderId;
            const div = document.createElement('div');
            div.className = `w-full text-left px-4 py-3 rounded-xl border transition-all flex items-center justify-between ${isSelected ? 'bg-blue-50 border-blue-200 ring-1 ring-blue-200' : 'bg-white border-gray-100 opacity-60'}`;
            div.innerHTML = `
                <div class="flex items-center gap-3">
                    <svg class="w-5 h-5 ${isSelected ? 'text-blue-600' : 'text-gray-400'}" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M3 7v10a2 2 0 002 2h14a2 2 0 002-2V9a2 2 0 00-2-2h-6l-2-2H5a2 2 0 00-2 2z"></path></svg>
                    <span class="text-sm font-bold ${isSelected ? 'text-blue-900' : 'text-gray-700'}">${f.name}</span>
                </div>
                ${isSelected ? '<span class="text-[9px] font-black uppercase text-blue-600 bg-white px-2 py-1 rounded-lg border border-blue-100">Actif</span>' : ''}
            `;
            this.folderListTarget.appendChild(div);
        });
    }

    async createNewProjectFolder() {
        this.folderLoaderTarget.classList.remove('hidden');
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}/drive-config/create-folder`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                credentials: 'include'
            });
            if (res.ok) {
                const folder = await res.json();
                this.selectedFolderId = folder.driveFolderId;
                this.generateFolderBtnTarget.classList.add('hidden');
                await this.loadFolders();
                alert(trans('project.create.drive.success.generated'));
            } else {
                const err = await res.json();
                alert(err.message || trans('project.create.drive.error.creation'));
            }
        } catch (e) { console.error(e); }
        finally { this.folderLoaderTarget.classList.add('hidden'); }
    }

    // --- MEMBER MANAGEMENT ---
    toggleRoleExplanation() {
        if (this.hasRoleExplanationTarget) {
            this.roleExplanationTarget.classList.toggle('hidden');
        }
    }

    addInvite() {
        const email = this.inviteEmailTarget.value.trim();
        if (!email || !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) return;
        if (this.invites.some(i => i.email === email)) return;
        this.invites.push({ email, role: 'MEMBER', isCreator: false, showMenu: false, isExisting: false });
        this.inviteEmailTarget.value = '';
        this.renderInvites();
    }
    toggleRoleMenu(event) { const idx = parseInt(event.currentTarget.dataset.index); this.invites[idx].showMenu = !this.invites[idx].showMenu; this.renderInvites(); }
    updateMemberRole(event) {
        const idx = parseInt(event.currentTarget.dataset.index);
        const newRole = event.currentTarget.dataset.role;
        
        // If promoting someone to ADMIN, demote the current ADMIN
        if (newRole === 'ADMIN') {
            this.invites.forEach((invite, i) => {
                if (i !== idx && invite.role === 'ADMIN') {
                    invite.role = 'MANAGER';
                    if (invite.isExisting) invite.roleChanged = true;
                }
            });
        }

        this.invites[idx].role = newRole;
        this.invites[idx].showMenu = false;
        if (this.invites[idx].isExisting) this.invites[idx].roleChanged = true;
        this.renderInvites();
    }
    removeInvite(event) { const idx = parseInt(event.currentTarget.dataset.index); if (!this.invites[idx].isCreator) { if (this.invites[idx].isExisting) this.removedMemberUuids.push(this.invites[idx].uuid); this.invites.splice(idx, 1); this.renderInvites(); } }
    renderInvites() {
        this.inviteListTarget.innerHTML = this.invites.map((invite, index) => {
            const isCreator = invite.isCreator;
            const roleLabel = trans(`project.create.form.members.role.${invite.role.toLowerCase()}`);
            
            return `
                <div class="flex items-center justify-between bg-gray-50 p-4 rounded-2xl border border-gray-100 ${isCreator ? 'border-bubblegum/20 bg-bubblegum/[0.02]' : ''}">
                    <div class="flex items-center gap-4">
                        <div class="w-10 h-10 rounded-xl bg-white shadow-sm flex items-center justify-center text-bubblegum font-black text-sm uppercase border border-gray-100">${invite.email.charAt(0).toUpperCase()}</div>
                        <div>
                            <p class="text-sm font-bold text-gray-900">${invite.email}</p>
                            <div class="relative">
                                <button type="button" 
                                        ${isCreator ? 'disabled' : `data-action="click->project-create#toggleRoleMenu" data-index="${index}"`}
                                        class="text-[10px] font-black text-gray-400 uppercase tracking-widest flex items-center gap-1 ${isCreator ? '' : 'hover:text-bubblegum'} transition-all text-left">
                                    ${roleLabel} ${invite.roleChanged ? '<span class="text-bubblegum lowercase font-bold">(modifié)</span>' : ''}
                                    ${!isCreator ? `<svg xmlns="http://www.w3.org/2000/svg" width="10" height="10" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"><path d="m6 9 6 6 6-6"/></svg>` : ''}
                                </button>
                                
                                ${invite.showMenu ? `
                                    <div class="absolute left-0 mt-2 w-40 bg-white rounded-xl shadow-xl border border-gray-100 z-50 py-1 overflow-hidden">
                                        <button type="button" data-action="click->project-create#updateMemberRole" data-index="${index}" data-role="ADMIN" class="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-gray-50 ${invite.role === 'ADMIN' ? 'text-bubblegum bg-bubblegum/5' : 'text-gray-500'}">
                                            ${trans('project.create.form.members.role.admin')}
                                        </button>
                                        <button type="button" data-action="click->project-create#updateMemberRole" data-index="${index}" data-role="MANAGER" class="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-gray-50 ${invite.role === 'MANAGER' ? 'text-bubblegum bg-bubblegum/5' : 'text-gray-500'}">
                                            ${trans('project.create.form.members.role.manager')}
                                        </button>
                                        <button type="button" data-action="click->project-create#updateMemberRole" data-index="${index}" data-role="MEMBER" class="w-full text-left px-4 py-2 text-[10px] font-black uppercase tracking-widest hover:bg-gray-50 ${invite.role === 'MEMBER' ? 'text-bubblegum bg-bubblegum/5' : 'text-gray-500'}">
                                            ${trans('project.create.form.members.role.member')}
                                        </button>
                                    </div>
                                ` : ''}
                            </div>
                        </div>
                    </div>
                    ${!isCreator ? `<button type="button" data-action="click->project-create#removeInvite" data-index="${index}" class="p-2 text-gray-300 hover:text-red-500 transition-all"><svg width="18" height="18" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2.5" d="M18 6 6 18M6 6l12 12"></path></svg></button>` : ''}
                </div>
            `;
        }).join('');
    }

    // --- SUBMISSION ---
    async submit() {
        this.spinnerTarget.classList.remove('hidden');
        this.submitBtnTarget.disabled = true;
        const selectedColorInput = this.colorInputsTargets.find(input => input.checked);
        const color = (selectedColorInput && selectedColorInput.value !== 'custom') ? selectedColorInput.value : this.customColorPickerTarget.value;
        
        let iconData = null;
        if (this.iconMode === 'SVG') {
            iconData = this.customSvgInputTarget.value.trim() || null;
        } else if (this.iconMode === 'EMOJI') {
            iconData = this.emojiInputTarget.value.trim() || null;
        } else if (this.iconMode === 'BLOB') {
            iconData = this.imagePreviewTarget.classList.contains('hidden') ? null : (this.imagePreviewTarget.src || null);
        }

        const projectData = { 
            title: this.nameTarget.value, 
            description: this.descriptionTarget.value, 
            status: this.hasStatusTarget ? this.statusTarget.value : 'ACTIVE', 
            color, 
            iconType: this.iconMode, 
            iconData
        };
        
        try {
            const isEdit = !!this.projectUuidValue;
            const res = await fetch(isEdit ? `${this.apiUrlValue}/projects/${this.projectUuidValue}` : `${this.apiUrlValue}/projects`, { 
                method: isEdit ? 'PUT' : 'POST', 
                headers: { 'Content-Type': 'application/json' }, 
                body: JSON.stringify(projectData), 
                credentials: 'include' 
            });
            
            if (res.ok) {
                const project = await res.json();
                const projectUuid = isEdit ? this.projectUuidValue : project.uuid;

                // --- Member management ---
                if (isEdit) {
                    for (const memberUuid of this.removedMemberUuids) {
                        await fetch(`${this.apiUrlValue}/projects/${projectUuid}/members/${memberUuid}`, {
                            method: 'DELETE',
                            credentials: 'include'
                        }).catch(e => console.error("Removal failed", e));
                    }

                    for (const member of this.invites) {
                        if (member.isExisting && member.roleChanged) {
                            await fetch(`${this.apiUrlValue}/projects/${projectUuid}/members/${member.uuid}`, {
                                method: 'PATCH',
                                headers: { 'Content-Type': 'application/json' },
                                body: JSON.stringify({ role: member.role }),
                                credentials: 'include'
                            }).catch(e => console.error("Role update failed", e));
                        }
                    }
                }

                // Send invitations for new entries
                for (const invite of this.invites) {
                    if (invite.isExisting || invite.isCreator) continue;
                    
                    await fetch(`${this.apiUrlValue}/projects/${projectUuid}/members/invite`, {
                        method: 'POST',
                        headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                        body: JSON.stringify({ email: invite.email, role: invite.role }),
                        credentials: 'include'
                    }).catch(e => console.error("Invitation failed", e));
                }

                window.location.href = `/projects/${projectUuid}`;
            } else {
                const err = await res.json();
                alert(err.message || trans('project.create.error.save'));
            }
        } catch (e) {
            console.error(e);
            alert(trans('project.create.error.network'));
        } finally {
            this.spinnerTarget.classList.add('hidden');
            this.submitBtnTarget.disabled = false;
        }
    }

    openDeleteModal() { this.deleteModalTarget.classList.remove('hidden'); }
    closeDeleteModal() { this.deleteModalTarget.classList.add('hidden'); }
    async deleteProject() {
        try {
            const res = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}`, { method: 'DELETE', credentials: 'include' });
            if (res.ok) window.location.href = '/dashboard';
        } catch (e) { console.error(e); }
    }
}
