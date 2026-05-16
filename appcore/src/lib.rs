use crate::client::Client;
use std::sync::{LazyLock, Mutex};

pub mod client;
pub mod features;
pub mod helpers;
pub mod java;
pub mod management;
pub mod utils;

pub static CLIENT: LazyLock<Mutex<Client>> = LazyLock::new(|| Mutex::new(Client::default()));
