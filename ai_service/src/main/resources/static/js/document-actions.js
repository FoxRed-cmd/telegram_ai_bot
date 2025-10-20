document.addEventListener('DOMContentLoaded', function () {
    const uploadForm = document.getElementById('upload-form');
    if (uploadForm) {
        uploadForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData(this);

            const progressContainer = document.getElementById('progress-container');
            const progressBar = document.getElementById('progress-bar');
            const progressText = document.getElementById('progress-text');

            progressContainer.classList.remove('d-none');
            progressBar.style.width = '0%';
            progressBar.textContent = '0%';
            progressText.textContent = 'Начинаем загрузку...';

            fetch('/documents/upload', { method: 'POST', body: formData })
                .then(response => response.json())
                .then(data => {
                    const taskId = data.taskId;
                    progressText.textContent = `Обработка ${data.totalPages} страниц...`;

                    const interval = setInterval(() => {
                        fetch(`/documents/progress/${taskId}`)
                            .then(r => r.json())
                            .then(status => {
                                if (!status) return;

                                const percent = Math.round((status.processedPages / status.totalPages) * 100);
                                progressBar.style.width = percent + '%';
                                progressBar.textContent = percent + '%';

                                if (status.finished) {
                                    clearInterval(interval);
                                    progressText.textContent = '✅ Обработка завершена!';
                                    setTimeout(() => location.reload(), 1500);
                                }
                            })
                            .catch(() => clearInterval(interval));
                    }, 1000);
                })
                .catch(() => {
                    progressText.textContent = '❌ Ошибка загрузки файла';
                });
        });
    }
});

function deleteDocument(button) {
    const filename = button.getAttribute('data-filename');
    if (!confirm(`Удалить документ "${filename}"?`)) return;

    button.disabled = true;
    const originalText = button.textContent;
    button.innerHTML = '<span class="spinner-border spinner-border-sm me-1" role="status" aria-hidden="true"></span> Удаляется...';

    fetch(`/documents/delete/${encodeURIComponent(filename)}`, {
        method: 'DELETE'
    })
        .then(response => {
            if (response.ok) {
                const row = button.closest('tr');
                row.style.transition = 'opacity 0.3s';
                row.style.opacity = '0';
                setTimeout(() => row.remove(), 300);
            } else {
                alert('Ошибка при удалении документа.');
                button.disabled = false;
                button.textContent = originalText;
            }
        })
        .catch(() => {
            alert('Ошибка при отправке запроса.');
            button.disabled = false;
            button.textContent = originalText;
        });
}
