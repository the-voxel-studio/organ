import { createTranslator } from '@symfony/ux-translator';
import { messages, localeFallbacks } from '@symfony/ux-translator/translations';

const translator = createTranslator({
    messages,
    localeFallbacks,
});

export const trans = (id, parameters, domain, locale) => translator.trans(id, parameters, domain, locale);
export default translator;
