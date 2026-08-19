use crate::account::Account;

pub fn ngram(accounts: &[Account], query: &str) -> Vec<Account> {
    let query = query.trim().to_lowercase();

    if query.is_empty() {
        return accounts.to_vec();
    }

    let ngrams = |text: &str| -> Vec<(char, char)> {
        let chars: Vec<char> = text.chars().collect();
        chars.windows(2).map(|pair| (pair[0], pair[1])).collect()
    };

    let query_ngrams = ngrams(&query);

    let similarity = |text: &str| -> f32 {
        let text = text.to_lowercase();

        if text.contains(&query) {
            return 1.0;
        }

        let text_ngrams = ngrams(&text);

        if query_ngrams.is_empty() || text_ngrams.is_empty() {
            return 0.0;
        }

        let shared = query_ngrams
            .iter()
            .filter(|ngram| text_ngrams.contains(ngram))
            .count();

        (2 * shared) as f32 / (query_ngrams.len() + text_ngrams.len()) as f32
    };

    let threshold: f32 = match query.len() {
        0..=3 => 0.3,
        4..=6 => 0.5,
        _ => 0.6,
    };

    let mut ranked: Vec<(&Account, f32)> = accounts
        .iter()
        .filter_map(|account| {
            let note = if account.note.to_lowercase().contains(&query) {
                1.0
            } else {
                0.0
            };

            let score = similarity(&account.name)
                .max(similarity(&account.issuer))
                .max(note);

            (score >= threshold).then_some((account, score))
        })
        .collect();

    ranked.sort_unstable_by(|a, b| b.1.total_cmp(&a.1));

    ranked
        .into_iter()
        .map(|(account, _)| account.clone())
        .collect()
}
