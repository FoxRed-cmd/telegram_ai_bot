document.addEventListener('DOMContentLoaded', function () {
    const uploadForm = document.getElementById('upload-form');
    const progressContainer = document.getElementById('progress-container');
    const progressBar = document.getElementById('progress-bar');
    const progressText = document.getElementById('progress-text');
    const uploadBtn = document.getElementById('upload-btn');
    const deleteButtons = document.querySelectorAll("button.btn-danger");
    let currentPollingInterval = null;

    const activeTaskId = localStorage.getItem('activeTaskId');
    if (activeTaskId) {
        resumeProgress(activeTaskId);
    }

    if (uploadForm) {
        uploadForm.addEventListener('submit', function (e) {
            e.preventDefault();
            const formData = new FormData(this);

            progressContainer.classList.remove('d-none');
            progressBar.style.width = '0%';
            progressBar.textContent = '0%';
            progressText.textContent = 'Начинаем загрузку...';
            progressText.style.color = '#8b949e';

            uploadBtn.disabled = true;
            deleteButtons.forEach(btn => btn.disabled = true);

            fetch('/documents/upload', { method: 'POST', body: formData })
                .then(response => response.json())
                .then(data => {
                    const taskId = data.taskId;
                    localStorage.setItem('activeTaskId', taskId);
                    progressText.textContent = `Обработка ${data.totalPages} страниц...`;
                    resumeProgress(taskId);
                })
                .catch((error) => {
                    console.error('Upload error:', error);
                    handleError('Ошибка загрузки файла');
                });
        });
    }

    function resumeProgress(taskId) {
        progressContainer.classList.remove('d-none');
        uploadBtn.disabled = true;
        deleteButtons.forEach(btn => btn.disabled = true);

        currentPollingInterval = setInterval(() => {
            fetch(`/documents/progress/${taskId}`)
                .then(r => r.json())
                .then(status => {
                    if (!status) return;

                    // Обработка ошибки
                    if (status.failed) {
                        handleError(status.errorMessage || 'Произошла ошибка при обработке');
                        return;
                    }

                    // Обработка отмены
                    if (status.cancelled) {
                        stopProgressPolling('Обработка прервана', currentPollingInterval);
                        return;
                    }

                    // Вычисляем прогресс
                    let percent = 0;
                    let progressMessage = '';

                    if (status.totalChunks > 0) {
                        // Этап 2: Векторизация чанков (от 0% до 100%)
                        percent = Math.round((status.embeddedChunks / status.totalChunks) * 100);
                        progressMessage = `Векторизация чанков: ${status.embeddedChunks} из ${status.totalChunks}`;
                    } else if (status.totalPages > 0) {
                        // Этап 1: Извлечение текста (от 0% до 100%)
                        percent = Math.round((status.processedPages / status.totalPages) * 100);
                        progressMessage = `Извлечение текста: ${status.processedPages} из ${status.totalPages} страниц`;
                    }

                    progressBar.style.width = percent + '%';
                    progressBar.textContent = percent + '%';
                    progressText.textContent = progressMessage;
                    progressText.style.color = '#8b949e';

                    if (status.finished) {
                        stopProgressPolling('Обработка завершена', currentPollingInterval);
                    }
                })
                .catch((error) => {
                    console.error('Polling error:', error);
                    handleError('Ошибка при получении статуса');
                });
        }, 1000);
    }

    function handleError(message) {
        if (currentPollingInterval) {
            clearInterval(currentPollingInterval);
            currentPollingInterval = null;
        }
        localStorage.removeItem('activeTaskId');
        
        progressBar.classList.remove('progress-bar-animated');
        progressBar.style.backgroundColor = '#f85149';
        progressBar.textContent = 'Ошибка';
        progressText.textContent = message;
        progressText.style.color = '#f85149';
        
        uploadBtn.disabled = false;
        deleteButtons.forEach(btn => btn.disabled = false);
        // Убрана автоматическая перезагрузка - пользователь сам решает когда перезагрузить страницу
    }

    function stopProgressPolling(textContent, interval) {
        clearInterval(interval);
        localStorage.removeItem('activeTaskId');
        progressText.textContent = textContent;
        setTimeout(() => location.reload(), 1500);
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

async function cancelProcess() {
    const activeTaskId = localStorage.getItem('activeTaskId');
    if (activeTaskId) {
        await fetch(`/documents/cancel/${activeTaskId}`, { method: "POST" });
    }
}
