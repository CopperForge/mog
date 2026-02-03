(() => {
    document.addEventListener('DOMContentLoaded', () => {
        document.querySelectorAll('[data-json-file]').forEach(input => {
            const form = input.closest('form');
            if (!form) {
                return;
            }
            const textarea = form.querySelector('textarea');
            if (!textarea) {
                return;
            }
            input.addEventListener('change', () => {
                const file = input.files && input.files[0];
                if (!file) {
                    return;
                }
                const reader = new FileReader();
                reader.onload = () => {
                    textarea.value = reader.result;
                };
                reader.readAsText(file);
            });
        });
    });
})();
