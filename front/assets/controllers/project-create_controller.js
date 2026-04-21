import { Controller } from '@hotwired/stimulus';
import { trans } from '../translator.js';

export default class extends Controller {
    static targets = [
        'name', 'description', 'status', 'colorInputs', 'customColorPicker', 'customColorDisplay', 'customColorIcon',
        'emojiInput', 'imageInput', 'imagePreview', 'imagePlaceholder', 'customSvgInput',
        'modeBtn', 'iconSection',
        'inviteEmail', 'inviteList', 'roleExplanation',
        'submitBtn', 'spinner', 'error',
        'deleteModal', 'deleteSubmitBtn', 'deleteSpinner'
    ];

    static values = {
        userEmail: String,
        userName: String,
        apiUrl: String,
        projectUuid: String,
        initialMembers: Array,
        invitations: Array
    };

    connect() {
        this.iconMode = 'BLOB';
        this.removedMemberUuids = [];
        
        // Handle ESC key to close modal
        this.escHandler = (e) => {
            if (e.key === 'Escape' && this.hasDeleteModalTarget && !this.deleteModalTarget.classList.contains('hidden')) {
                this.closeDeleteModal();
            }
        };
        window.addEventListener('keydown', this.escHandler);
        
        if (this.hasProjectUuidValue && this.projectUuidValue) {
            // Edit mode: Load existing members
            const existingMembers = this.initialMembersValue.map(m => {
                const isCreator = (m.user.email === this.userEmailValue);
                return {
                    uuid: m.uuid, // Existing membership UUID
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

            // Load pending invitations
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
            
            this.invites = [...existingMembers, ...pendingInvites];
            this.invites.sort((a, b) => b.isCreator - a.isCreator);

            // Detect icon mode
            if (!this.imagePreviewTarget.classList.contains('hidden')) this.iconMode = 'BLOB';
            else if (this.emojiInputTarget.value) this.iconMode = 'EMOJI';
            else if (this.customSvgInputTarget.value) this.iconMode = 'SVG';

        } else {
            // New mode
            this.invites = [
                { 
                    email: this.userEmailValue, 
                    name: this.userNameValue, 
                    role: 'ADMIN', 
                    isCreator: true,
                    showMenu: false,
                    isExisting: false,
                    isPending: false
                }
            ];
        }
        
        this.renderInvites();
    }

    // --- COLOR MANAGEMENT ---
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

    // --- ICON MANAGEMENT ---
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

    validateEmojiInput(event) {
        const val = event.target.value.trim();
        if (!val) return;
        const segmenter = new Intl.Segmenter(undefined, { granularity: 'grapheme' });
        const segments = Array.from(segmenter.segment(val));
        const firstGrapheme = segments[0]?.segment;
        const emojiRegex = /\p{Extended_Pictographic}/u;

        if (firstGrapheme && emojiRegex.test(firstGrapheme)) {
            event.target.value = firstGrapheme;
            this.errorTarget.classList.add('hidden');
        } else {
            event.target.value = '';
            this.showError("Veuillez saisir un émoji valide.");
        }
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

    // --- MEMBER MANAGEMENT ---
    addInvite() {
        const email = this.inviteEmailTarget.value.trim();
        if (!email || !this.validateEmail(email)) {
            this.showError("Veuillez saisir une adresse email valide.");
            return;
        }
        if (this.invites.some(invite => invite.email === email)) {
            this.showError("Cet utilisateur est déjà dans la liste.");
            return;
        }
        this.errorTarget.classList.add('hidden');
        this.invites.push({ email, role: 'MEMBER', isCreator: false, showMenu: false, isExisting: false });
        this.inviteEmailTarget.value = '';
        this.renderInvites();
    }

    toggleRoleMenu(event) {
        const index = parseInt(event.currentTarget.dataset.index);
        this.invites[index].showMenu = !this.invites[index].showMenu;
        this.renderInvites();
    }

    updateMemberRole(event) {
        const index = parseInt(event.currentTarget.dataset.index);
        const role = event.currentTarget.dataset.role;
        if (this.invites[index]) {
            this.invites[index].role = role;
            this.invites[index].showMenu = false;
            if (this.invites[index].isExisting) {
                this.invites[index].roleChanged = true;
            }
            this.renderInvites();
        }
    }

    removeInvite(event) {
        const index = parseInt(event.currentTarget.dataset.index);
        if (this.invites[index] && !this.invites[index].isCreator) {
            if (this.invites[index].isExisting) {
                this.removedMemberUuids.push(this.invites[index].uuid);
            }
            this.invites.splice(index, 1);
            this.renderInvites();
        }
    }

    renderInvites() {
        if (this.invites.length === 0) {
            this.inviteListTarget.innerHTML = `<p class="text-sm text-gray-400 italic">${trans('project.create.form.members.no_members')}</p>`;
            return;
        }

        this.inviteListTarget.innerHTML = this.invites.map((invite, index) => {
            const roleLabel = trans('project.create.form.members.role.' + invite.role.toLowerCase());
            
            return `
                <div class="flex items-center justify-between bg-gray-50 p-4 rounded-2xl border border-gray-100 group animate-in slide-in-from-left-2 ${invite.isCreator ? 'border-bubblegum/20 bg-bubblegum/[0.02]' : ''}">
                    <div class="flex items-center gap-4">
                        <div class="w-10 h-10 rounded-xl bg-white shadow-sm flex items-center justify-center text-bubblegum font-black text-sm uppercase border border-gray-100">
                            ${invite.email.charAt(0).toUpperCase()}
                        </div>
                        <div>
                            <div class="flex items-center gap-2">
                                <p class="text-sm font-bold text-gray-900 truncate max-w-[200px]">${invite.email}</p>
                                ${invite.isCreator ? '<span class="text-[10px] font-black uppercase tracking-widest text-bubblegum bg-bubblegum/10 px-2 py-0.5 rounded-md">Propriétaire</span>' : ''}
                                ${invite.isPending ? '<span class="text-[10px] font-black uppercase tracking-widest text-amber-600 bg-amber-50 border border-amber-100 px-2 py-0.5 rounded-md">En attente</span>' : ''}
                            </div>
                            <p class="text-[10px] font-black text-gray-400 uppercase tracking-widest mt-0.5">
                                ${roleLabel} ${invite.roleChanged ? '<span class="text-bubblegum font-bold ml-1">(Modifié)</span>' : ''}
                            </p>
                        </div>
                    </div>
                    
                    <div class="flex items-center gap-2">
                        ${(!invite.isCreator && !invite.isPending) ? `
                            <div class="relative">
                                <button type="button" class="px-3 py-1.5 rounded-lg text-[10px] font-black uppercase tracking-widest ${invite.showMenu ? 'text-bubblegum bg-white border-gray-100' : 'text-gray-500 hover:text-bubblegum hover:bg-white'} transition-all border border-transparent"
                                        data-action="click->project-create#toggleRoleMenu" data-index="${index}">
                                    Modifier le rôle
                                </button>
                                ${invite.showMenu ? `
                                    <div class="absolute right-0 bottom-full mb-2 bg-white border border-gray-100 shadow-2xl rounded-xl p-1 z-50 min-w-[120px]">
                                        <button type="button" data-action="click->project-create#updateMemberRole" data-index="${index}" data-role="MANAGER" class="w-full text-left px-3 py-2 text-[10px] font-bold uppercase tracking-wider hover:bg-bubblegum/5 hover:text-bubblegum rounded-lg transition-colors">Manager</button>
                                        <button type="button" data-action="click->project-create#updateMemberRole" data-index="${index}" data-role="MEMBER" class="w-full text-left px-3 py-2 text-[10px] font-bold uppercase tracking-wider hover:bg-bubblegum/5 hover:text-bubblegum rounded-lg transition-colors">Membre</button>
                                    </div>
                                ` : ''}
                            </div>

                            <button type="button" data-action="click->project-create#removeInvite" data-index="${index}" 
                                    class="p-2 text-gray-300 hover:text-red-500 transition-all">
                                <svg xmlns="http://www.w3.org/2000/svg" width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></svg>
                            </button>
                        ` : ''}
                    </div>
                </div>
            `;
        }).join('');
    }

    toggleRoleExplanation() {
        this.roleExplanationTarget.classList.toggle('hidden');
    }

    // --- DELETE MODAL ---
    openDeleteModal() {
        this.deleteModalTarget.classList.remove('hidden');
        document.body.style.overflow = 'hidden';
    }

    closeDeleteModal() {
        if (this.deleteSubmitBtnTarget.disabled) return;
        this.deleteModalTarget.classList.add('hidden');
        document.body.style.overflow = 'auto';
    }

    // --- SUBMISSION ---
    async deleteProject() {
        this.deleteSubmitBtnTarget.disabled = true;
        this.deleteSpinnerTarget.classList.remove('hidden');
        this.errorTarget.classList.add('hidden');

        try {
            const response = await fetch(`${this.apiUrlValue}/projects/${this.projectUuidValue}`, {
                method: 'DELETE',
                credentials: 'include'
            });

            if (!response.ok) {
                const errorData = await response.json().catch(() => ({}));
                throw new Error(errorData.message || "Erreur lors de la suppression.");
            }

            // Invalidation du cache côté client (optionnel selon ton setup)
            window.location.href = '/dashboard';
        } catch (e) {
            this.showError(e.message);
            this.deleteSubmitBtnTarget.disabled = false;
            this.deleteSpinnerTarget.classList.add('hidden');
            this.closeDeleteModal();
        }
    }

    async submit() {
        this.errorTarget.classList.add('hidden');
        const name = this.nameTarget.value.trim();
        if (!name) {
            this.showError("Le nom du projet est obligatoire.");
            return;
        }

        this.submitBtnTarget.disabled = true;
        this.spinnerTarget.classList.remove('hidden');

        const selectedColorInput = this.colorInputsTargets.find(input => input.checked);
        const color = (selectedColorInput && selectedColorInput.value !== 'custom') 
            ? selectedColorInput.value 
            : this.customColorPickerTarget.value;
        
        let iconType = this.iconMode;
        let iconData = '';

        if (this.iconMode === 'SVG') {
            iconData = this.customSvgInputTarget.value.trim();
        } else if (this.iconMode === 'EMOJI') {
            iconData = this.emojiInputTarget.value || '🚀';
        } else if (this.iconMode === 'BLOB') {
            iconData = this.imagePreviewTarget.src;
        }

        const projectData = {
            title: name,
            description: this.descriptionTarget.value.trim(),
            status: this.hasStatusTarget ? this.statusTarget.value : 'ACTIVE',
            color: color,
            iconType: iconType,
            iconData: iconData
        };

        const isEdit = this.hasProjectUuidValue && this.projectUuidValue;
        const url = isEdit ? `${this.apiUrlValue}/projects/${this.projectUuidValue}` : `${this.apiUrlValue}/projects`;
        const method = isEdit ? 'PUT' : 'POST';

        try {
            const projectResponse = await fetch(url, {
                method: method,
                headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                body: JSON.stringify(projectData),
                credentials: 'include'
            });

            if (!projectResponse.ok) {
                const errorData = await projectResponse.json().catch(() => ({}));
                throw new Error(errorData.error || errorData.message || "Erreur lors de la sauvegarde.");
            }

            const project = await projectResponse.json();
            const projectUuid = isEdit ? this.projectUuidValue : project.uuid;

            // --- Member management in Edit Mode ---
            if (isEdit) {
                // 1. Delete removed members
                for (const memberUuid of this.removedMemberUuids) {
                    await fetch(`${this.apiUrlValue}/projects/${projectUuid}/members/${memberUuid}`, {
                        method: 'DELETE',
                        credentials: 'include'
                    }).catch(e => console.error("Removal failed for member", memberUuid));
                }

                // 2. Update roles for existing members
                for (const member of this.invites) {
                    if (member.isExisting && member.roleChanged) {
                        await fetch(`${this.apiUrlValue}/projects/${projectUuid}/members/${member.uuid}`, {
                            method: 'PATCH',
                            headers: { 'Content-Type': 'application/json' },
                            body: JSON.stringify({ role: member.role }),
                            credentials: 'include'
                        }).catch(e => console.error("Role update failed for member", member.email));
                    }
                }
            }

            // 3. Send new invitations
            for (const invite of this.invites) {
                if (invite.isExisting || invite.isCreator) continue;
                
                await fetch(`${this.apiUrlValue}/projects/${projectUuid}/members/invite`, {
                    method: 'POST',
                    headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
                    body: JSON.stringify({ email: invite.email, role: invite.role }),
                    credentials: 'include'
                }).catch(e => console.error("Invitation failed for", invite.email));
            }

            window.location.href = isEdit ? `/projects/${projectUuid}` : '/dashboard';

        } catch (e) {
            this.showError(e.message);
        }
    }

    showError(message) {
        this.errorTarget.innerText = message;
        this.errorTarget.classList.remove('hidden');
        this.submitBtnTarget.disabled = false;
        this.spinnerTarget.classList.add('hidden');
        this.errorTarget.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }

    validateEmail(email) {
        return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email);
    }
}
