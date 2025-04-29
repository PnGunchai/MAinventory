import React from 'react';
import { LentOrderStatus } from '../types/lent';

interface LentOrderStatusBadgeProps {
    status: LentOrderStatus;
}

export const LentOrderStatusBadge: React.FC<LentOrderStatusBadgeProps> = ({ status }) => {
    const getStatusColor = () => {
        switch (status) {
            case 'active':
                return 'bg-yellow-100 text-yellow-800';
            case 'completed':
                return 'bg-green-100 text-green-800';
            default:
                return 'bg-gray-100 text-gray-800';
        }
    };

    return (
        <span className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium ${getStatusColor()}`}>
            {status.charAt(0).toUpperCase() + status.slice(1)}
        </span>
    );
}; 