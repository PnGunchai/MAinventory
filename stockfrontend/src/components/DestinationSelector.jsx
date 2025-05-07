import React from 'react';
import { SplitDestinationInput } from './SplitDestinationInput';
import { useTranslation } from 'react-i18next';
import Button from '@/components/Button';

export const DestinationSelector = ({
    identifier,
    isNonSerialized,
    quantity,
    initialDestination,
    onChange
}) => {
    const { t } = useTranslation();
    const handleSingleDestinationChange = (destination) => {
        onChange(identifier, { destination });
    };
    const handleSplitDestinationsChange = (identifier, splitDestinations) => {
        onChange(identifier, { destination: 'split', splitDestinations });
    };
    return (
        <div className="space-y-4">
            {isNonSerialized ? (
                <SplitDestinationInput
                    identifier={identifier}
                    totalQuantity={quantity}
                    initialSplitDestinations={initialDestination?.splitDestinations}
                    onChange={handleSplitDestinationsChange}
                />
            ) : (
                <div className="flex space-x-4">
                    <Button
                        onClick={() => handleSingleDestinationChange('return')}
                        className={`px-4 py-2 rounded-md ${
                            initialDestination?.destination === 'return'
                                ? 'bg-blue-600 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                        {t('return')}
                    </Button>
                    <Button
                        onClick={() => handleSingleDestinationChange('sales')}
                        className={`px-4 py-2 rounded-md ${
                            initialDestination?.destination === 'sales'
                                ? 'bg-blue-600 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                        {t('sales')}
                    </Button>
                    <Button
                        onClick={() => handleSingleDestinationChange('broken')}
                        className={`px-4 py-2 rounded-md ${
                            initialDestination?.destination === 'broken'
                                ? 'bg-blue-600 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                        }`}
                    >
                        {t('broken')}
                    </Button>
                </div>
            )}
        </div>
    );
}; 