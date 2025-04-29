/**
 * Format a date string or timestamp into a localized date and time string
 * @param {string|Date} date - The date to format
 * @returns {string} The formatted date string
 */
export const formatDateTime = (date) => {
    if (!date) return '';
    
    const dateObj = typeof date === 'string' ? new Date(date) : date;
    
    return dateObj.toLocaleString('th-TH', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        hour12: false
    });
}; 