import { toZonedTime, format } from 'date-fns-tz';

/**
 * Format a date string or timestamp into a localized date and time string
 * @param {string|Date} date - The date to format
 * @returns {string} The formatted date string
 */
export const formatDateTime = (date) => {
    if (!date) return '';
    
    // Parse as UTC if no timezone info
    let dateObj = typeof date === 'string' && !date.endsWith('Z') && !date.includes('+')
        ? new Date(date + 'Z')
        : new Date(date);

    const timeZone = 'Asia/Bangkok';
    const zonedDate = toZonedTime(dateObj, timeZone);
    return format(zonedDate, 'yyyy-MM-dd HH:mm', { timeZone });
}; 