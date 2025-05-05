import React, { useState, useEffect } from 'react';
import { useTranslation } from 'react-i18next';

export const SplitDestinationInput = ({
    identifier,
    totalQuantity,
    initialSplitDestinations,
    onChange
}) => {
    const { t } = useTranslation();
    const [splitDestinations, setSplitDestinations] = useState(
        initialSplitDestinations || { return: 0, sales: 0, broken: 0 }
    );
    const [error, setError] = useState(null);

    useEffect(() => {
        if (initialSplitDestinations) {
            setSplitDestinations(initialSplitDestinations);
        }
    }, [initialSplitDestinations]);

    const handleQuantityChange = (destination, value) => {
        const newQuantity = parseInt(value) || 0;
        const newSplitDestinations = { ...splitDestinations, [destination]: newQuantity };
        const total = Object.values(newSplitDestinations).reduce((sum, qty) => sum + (qty || 0), 0);
        if (total > totalQuantity) {
            setError(t('totalQuantityCannotExceed', { totalQuantity }));
            return;
        }
        setError(null);
        setSplitDestinations(newSplitDestinations);
        onChange(identifier, newSplitDestinations);
    };

    const remainingQuantity = totalQuantity - Object.values(splitDestinations)
        .reduce((sum, qty) => sum + (qty || 0), 0);

    return (
        <div className="space-y-4 p-4 bg-gray-50 rounded-lg">
            <div className="text-sm text-gray-600">
                {t('totalQuantity')}: {totalQuantity} | {t('remaining')}: {remainingQuantity}
            </div>
            {error && (
                <div className="text-red-600 text-sm">{error}</div>
            )}
            <div className="grid grid-cols-3 gap-4">
                <div>
                    <label className="block text-sm font-medium text-gray-700">
                        {t('return')}
                    </label>
                    <input
                        type="number"
                        min="0"
                        max={totalQuantity}
                        value={splitDestinations.return || ''}
                        onChange={(e) => handleQuantityChange('return', e.target.value)}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700">
                        {t('sales')}
                    </label>
                    <input
                        type="number"
                        min="0"
                        max={totalQuantity}
                        value={splitDestinations.sales || ''}
                        onChange={(e) => handleQuantityChange('sales', e.target.value)}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    />
                </div>
                <div>
                    <label className="block text-sm font-medium text-gray-700">
                        {t('broken')}
                    </label>
                    <input
                        type="number"
                        min="0"
                        max={totalQuantity}
                        value={splitDestinations.broken || ''}
                        onChange={(e) => handleQuantityChange('broken', e.target.value)}
                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-blue-500 focus:ring-blue-500 sm:text-sm"
                    />
                </div>
            </div>
        </div>
    );
}; 