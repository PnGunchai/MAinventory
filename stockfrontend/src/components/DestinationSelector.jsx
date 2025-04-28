import React from 'react';
import { SingleDestination, ItemDestination, SplitDestinations } from '../types/lent';
import { SplitDestinationInput } from './SplitDestinationInput';

interface DestinationSelectorProps {
    identifier: string;
    isNonSerialized: boolean;
    quantity: number;
    initialDestination?: ItemDestination;
    onChange: (identifier: string, destination: ItemDestination) => void;
}

export const DestinationSelector: React.FC<DestinationSelectorProps> = ({
    identifier,
    isNonSerialized,
    quantity,
    initialDestination,
    onChange
}) => {
    const handleSingleDestinationChange = (destination: SingleDestination) => {
        onChange(identifier, { destination });
    };

    const handleSplitDestinationsChange = (identifier: string, splitDestinations: SplitDestinations) => {
        onChange(identifier, { destination: 'split', splitDestinations });
    };

    return (
        <div className="space-y-4">
            {isNonSerialized ? (
                // For non-serialized products, show split destination inputs
                <SplitDestinationInput
                    identifier={identifier}
                    totalQuantity={quantity}
                    initialSplitDestinations={initialDestination?.splitDestinations}
                    onChange={handleSplitDestinationsChange}
                />
            ) : (
                // For serialized products, show single destination selector
                <div className="flex space-x-4">
                    <button
                        onClick={() => handleSingleDestinationChange('return')}
                        className={`px-4 py-2 rounded-md ${
                            initialDestination?.destination === 'return'
                                ? 'bg-blue-600 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                        Return
                    </button>
                    <button
                        onClick={() => handleSingleDestinationChange('sales')}
                        className={`px-4 py-2 rounded-md ${
                            initialDestination?.destination === 'sales'
                                ? 'bg-blue-600 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                        Sales
                    </button>
                    <button
                        onClick={() => handleSingleDestinationChange('broken')}
                        className={`px-4 py-2 rounded-md ${
                            initialDestination?.destination === 'broken'
                                ? 'bg-blue-600 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                        Broken
                    </button>
                </div>
            )}
        </div>
    );
}; 