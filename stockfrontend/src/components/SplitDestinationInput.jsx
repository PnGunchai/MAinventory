import React, { useState, useEffect } from 'react';
import { SplitDestinations } from '../types/lent';

interface SplitDestinationInputProps {
    identifier: string;
    totalQuantity: number;
    initialSplitDestinations?: SplitDestinations;
    onChange: (identifier: string, splitDestinations: SplitDestinations) => void;
}

export const SplitDestinationInput: React.FC<SplitDestinationInputProps> = ({
    identifier,
    totalQuantity,
    initialSplitDestinations,
    onChange
}) => {
    const [splitDestinations, setSplitDestinations] = useState<SplitDestinations>(
        initialSplitDestinations || { return: 0, sales: 0, broken: 0 }
    );

    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        // Update local state when initialSplitDestinations changes
        if (initialSplitDestinations) {
            setSplitDestinations(initialSplitDestinations);
        }
    }, [initialSplitDestinations]);

    const handleQuantityChange = (destination: keyof SplitDestinations, value: string) => {
        const newQuantity = parseInt(value) || 0;
        const newSplitDestinations = { ...splitDestinations, [destination]: newQuantity };

        // Calculate total of all destinations
        const total = Object.values(newSplitDestinations).reduce((sum, qty) => sum + (qty || 0), 0);

        if (total > totalQuantity) {
            setError(`Total quantity cannot exceed ${totalQuantity}`);
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
                Total Quantity: {totalQuantity} | Remaining: {remainingQuantity}
            </div>
            
            {error && (
                <div className="text-red-600 text-sm">{error}</div>
            )}
            
            <div className="grid grid-cols-3 gap-4">
                {/* Return Input */}
                <div>
                    <label className="block text-sm font-medium text-gray-700">
                        Return
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

                {/* Sales Input */}
                <div>
                    <label className="block text-sm font-medium text-gray-700">
                        Sales
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

                {/* Broken Input */}
                <div>
                    <label className="block text-sm font-medium text-gray-700">
                        Broken
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